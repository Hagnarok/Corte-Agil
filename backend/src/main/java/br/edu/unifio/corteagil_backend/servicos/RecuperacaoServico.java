package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.repositorios.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecuperacaoServico {
  private final UsuarioRepositorio usuarios;
  private final RedefinicaoRepositorio redefinicoes;
  private final AuditoriaServico auditoria;
  private final ObjectProvider<JavaMailSender> mail;
  private final Environment env;

  private String hash(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String link(Usuario u) {
    String base = env.getProperty("PUBLIC_BASE_URL", "");
    if (base.isBlank())
      throw new RegraNegocioException(
          "Endereço público de recuperação não configurado. Consulte o administrador.", 503);
    var uri = java.net.URI.create(base);
    if (uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || (!"https".equals(uri.getScheme())
            && !("http".equals(uri.getScheme())
                && List.of("localhost", "127.0.0.1").contains(uri.getHost()))))
      throw new RegraNegocioException("Configure um endereço HTTPS válido.", 503);
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var r = new RedefinicaoSenha();
    r.setHash(hash(token));
    r.setUsuario(u);
    r.setExpiraEm(System.currentTimeMillis() + 900000);
    redefinicoes.save(r);
    return base.replaceAll("/+$", "") + "/recuperar-senha#token=" + token;
  }

  @Transactional
  public String gerar(String id, Usuario ator) {
    var u =
        usuarios
            .findById(id)
            .filter(x -> x.isAtivo())
            .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado.", 404));
    String url = link(u);
    auditoria.registrar("Link de redefinição gerado", u.getUsername(), ator);
    return url;
  }

  @Transactional
  public void solicitar(String email) {
    var sender = mail.getIfAvailable();
    if (sender == null)
      throw new RegraNegocioException(
          "Recuperação por e-mail indisponível. Solicite um link ao administrador.", 503);
    var u = usuarios.findByEmail(email.trim().toLowerCase()).filter(x -> x.isAtivo()).orElse(null);
    if (u == null) return;
    String url = link(u);
    var msg = new SimpleMailMessage();
    msg.setFrom(env.getProperty("MAIL_FROM", env.getProperty("spring.mail.username", "")));
    msg.setTo(u.getEmail());
    msg.setSubject("Redefinição de senha - Corte Ágil");
    msg.setText(
        "Abra este link para definir sua senha. Ele expira em 15 minutos e só pode ser utilizado uma vez:\n\n"
            + url);
    try {
      sender.send(msg);
    } catch (org.springframework.mail.MailException e) {
      throw new RegraNegocioException(
          "Não foi possível enviar o e-mail. Tente novamente ou solicite ajuda ao administrador.",
          503);
    }
  }

  @Transactional
  public void redefinir(String token, String senha) {
    var r =
        redefinicoes
            .paraRedefinir(hash(token))
            .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado.", 400));
    if (r.isUsado() || r.getExpiraEm() < System.currentTimeMillis() || !r.getUsuario().isAtivo())
      throw new RegraNegocioException("Link inválido ou expirado.", 400);
    if (senha.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException("A senha excede 72 bytes.", 400);
    var u = r.getUsuario();
    u.setPasswordHash(new BCryptPasswordEncoder().encode(senha));
    u.setVersaoSessao(u.getVersaoSessao() + 1);
    r.setUsado(true);
    auditoria.registrar("Senha redefinida", u.getUsername(), u);
  }
}
