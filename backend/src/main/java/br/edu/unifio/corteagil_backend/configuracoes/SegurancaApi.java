package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.servicos.AutenticacaoServico;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SegurancaApi {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private static void erro(HttpServletResponse res, int status, String mensagem)
      throws IOException {
    res.setStatus(status);
    if (status == 401) res.setHeader("WWW-Authenticate", "Basic realm=\"CorteAgil\"");
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"message\":\"" + mensagem + "\"}");
  }

  @Bean
  SecurityFilterChain api(HttpSecurity http, AutenticacaoServico auth) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .securityContext(
            c -> c.securityContextRepository(new RequestAttributeSecurityContextRepository()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/api/queue/**", "/api/audit", "/api/gestao/**", "/api/consultas/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .httpBasic(
            b ->
                b.authenticationEntryPoint(
                    (req, res, e) -> {
                      res.setHeader("WWW-Authenticate", "Basic realm=\"CorteAgil\"");
                      erro(res, 401, "Entre como funcionário para continuar.");
                    }))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> erro(res, 401, "Entre como funcionário para continuar."))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            erro(res, 403, "Você não tem permissão para esta operação.")))
        .logout(l -> l.disable())
        .addFilterBefore(new LimiteBasic(), BasicAuthenticationFilter.class)
        .addFilterBefore(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                  throws ServletException, IOException {
                if (SecurityContextHolder.getContext().getAuthentication() == null
                    && req.getSession(false) != null) {
                  try {
                    var u = auth.atual(req);
                    var user =
                        User.withUsername(u.getUsername())
                            .password(u.getPasswordHash())
                            .roles(u.getPerfil())
                            .build();
                    SecurityContextHolder.getContext()
                        .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities()));
                  } catch (RegraNegocioException ignored) {
                  }
                }
                chain.doFilter(req, res);
              }
            },
            AnonymousAuthenticationFilter.class);
    return http.build();
  }

  private static class LimiteBasic extends OncePerRequestFilter {
    private record Falhas(long ate, int quantidade) {}

    private final ConcurrentHashMap<String, Falhas> falhas = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
      String header = req.getHeader("Authorization");
      if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
        chain.doFilter(req, res);
        return;
      }
      long now = System.currentTimeMillis();
      falhas.entrySet().removeIf(e -> e.getValue().ate() < now);
      String key = req.getRemoteAddr();
      var atual = falhas.get(key);
      if (atual != null && atual.quantidade() >= 8) {
        erro(res, 429, "Muitas tentativas. Aguarde um minuto.");
        return;
      }
      chain.doFilter(req, res);
      if (res.getStatus() == 401)
        falhas.compute(
            key,
            (k, v) ->
                v == null || v.ate() < now
                    ? new Falhas(now + 60000, 1)
                    : new Falhas(v.ate(), v.quantidade() + 1));
    }
  }
}
