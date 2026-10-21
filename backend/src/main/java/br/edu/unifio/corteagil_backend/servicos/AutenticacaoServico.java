package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.dto.LoginRequest;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoServico {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final String hash;

  public AutenticacaoServico(Environment env) {
    String password = env.getProperty("ADMIN_PASSWORD", "");
    if (password.length() < 10)
      throw new IllegalStateException(
          "Configure ADMIN_PASSWORD no .env com pelo menos 10 caracteres.");
    hash = encoder.encode(password);
  }

  public void login(LoginRequest input, HttpServletRequest request) {
    boolean matches = encoder.matches(input.password(), hash);
    if (!input.username().equals("admin") || !matches)
      throw new RegraNegocioException("Usuário ou senha incorretos.", 401);
    var old = request.getSession(false);
    if (old != null) old.invalidate();
    request.getSession(true).setAttribute("username", "admin");
  }

  public void require(HttpServletRequest req) {
    var session = req.getSession(false);
    if (session == null || !"admin".equals(session.getAttribute("username")))
      throw new RegraNegocioException("Entre como funcionário para continuar.", 401);
  }

  public void logout(HttpServletRequest req) {
    require(req);
    req.getSession(false).invalidate();
  }
}
