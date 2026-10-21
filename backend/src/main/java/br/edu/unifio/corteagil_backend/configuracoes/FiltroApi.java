package br.edu.unifio.corteagil_backend.configuracoes;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroApi extends OncePerRequestFilter {
  private final Set<String> origins = new HashSet<>();
  private final Map<String, Window> rates = new ConcurrentHashMap<>();

  private record Window(long until, int count) {}

  public FiltroApi(Environment env) {
    for (String origin : env.getProperty("ALLOWED_ORIGINS", "").split(","))
      if (!origin.isBlank()) origins.add(origin.trim());
    origins.add("http://localhost:4200");
    origins.add("http://127.0.0.1:4200");
    String name = env.getProperty("CODESPACE_NAME");
    String port = env.getProperty("PORT", "3000");
    if (name != null && !name.isBlank()) {
      origins.add("https://localhost:" + port);
      for (String p : List.of(port, "4200"))
        origins.add(
            "https://"
                + name
                + "-"
                + p
                + "."
                + env.getProperty("GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN", "app.github.dev"));
    }
  }

  private void reject(HttpServletResponse res, int status, String text) throws IOException {
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"message\":\"" + text + "\"}");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Referrer-Policy", "no-referrer");
    if (req.getRequestURI().startsWith("/api/")) {
      res.setHeader("Cache-Control", "no-store");
      if (req.getMethod().equals("POST")) {
        String origin = req.getHeader("Origin");
        String own = req.getScheme() + "://" + req.getHeader("Host");
        if (origin != null && !origin.equals(own) && !origins.contains(origin)) {
          reject(res, 403, "Origem não permitida.");
          return;
        }
        long now = System.currentTimeMillis();
        String path = req.getRequestURI();
        int max =
            path.equals("/api/login")
                ? 8
                : path.equals("/api/tickets") ? 60 : path.endsWith("/return") ? 20 : 300;
        String key = req.getRemoteAddr() + ":" + path;
        Window window =
            rates.compute(
                key,
                (k, v) ->
                    v == null || v.until < now
                        ? new Window(now + 60000, 1)
                        : new Window(v.until, v.count + 1));
        rates.entrySet().removeIf(e -> e.getValue().until < now);
        if (window.count > max) {
          reject(res, 429, "Muitas solicitações. Aguarde um minuto.");
          return;
        }
      }
    }
    chain.doFilter(req, res);
  }
}
