package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.dto.LoginRequest;
import br.edu.unifio.corteagil_backend.entidades.Usuario;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.repositorios.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacaoServico {
  private final UsuarioRepositorio usuarios;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public void login(LoginRequest input, HttpServletRequest request) {
    if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException("Usuário ou senha incorretos.", 401);
    var u = usuarios.findByUsername(input.username()).orElse(null);
    boolean ok =
        encoder.matches(
            input.password(),
            u == null
                ? "$2a$10$abcdefghijklmnopqrstuu1GZ8VY4N8y9w7V8Iu2DpY9nZNSOQdm4G"
                : u.getPasswordHash());
    if (u == null || !ok || !u.isAtivo())
      throw new RegraNegocioException("Usuário ou senha incorretos.", 401);
    var old = request.getSession(false);
    if (old != null) old.invalidate();
    var sess = request.getSession(true);
    sess.setAttribute("username", u.getUsername());
    sess.setAttribute("usuarioId", u.getId());
    sess.setAttribute("versao", u.getVersaoSessao());
  }

  public Usuario atual(HttpServletRequest req) {
    var authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal()
            instanceof org.springframework.security.core.userdetails.UserDetails principal) {
      return usuarios
          .findByUsername(principal.getUsername())
          .filter(Usuario::isAtivo)
          .orElseThrow(() -> new RegraNegocioException("Sessão expirada. Entre novamente.", 401));
    }
    var s = req.getSession(false);
    if (s == null) throw new RegraNegocioException("Entre como funcionário para continuar.", 401);
    var u = usuarios.findById(String.valueOf(s.getAttribute("usuarioId"))).orElse(null);
    if (u == null
        || !u.isAtivo()
        || !Long.valueOf(u.getVersaoSessao()).equals(s.getAttribute("versao")))
      throw new RegraNegocioException("Sessão expirada. Entre novamente.", 401);
    return u;
  }

  public void require(HttpServletRequest req) {
    atual(req);
  }

  public Usuario admin(HttpServletRequest req) {
    var u = atual(req);
    if (!u.getPerfil().equals("ADMIN"))
      throw new RegraNegocioException("Acesso exclusivo do administrador.", 403);
    return u;
  }

  public void logout(HttpServletRequest req) {
    require(req);
    var session = req.getSession(false);
    if (session != null) session.invalidate();
  }
}
