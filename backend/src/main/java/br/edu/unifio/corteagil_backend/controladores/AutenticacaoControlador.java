package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.dto.LoginRequest;
import br.edu.unifio.corteagil_backend.servicos.AutenticacaoServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AutenticacaoControlador {
  private final AutenticacaoServico auth;

  @PostMapping("/login")
  public Object login(@Valid @RequestBody LoginRequest input, HttpServletRequest req) {
    auth.login(input, req);
    return Map.of("ok", true);
  }

  @GetMapping("/session")
  public Object session(HttpServletRequest req) {
    auth.require(req);
    return Map.of("username", "admin");
  }

  @PostMapping("/logout")
  public Object logout(HttpServletRequest req) {
    auth.logout(req);
    return Map.of("ok", true);
  }
}
