package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RecuperacaoControlador {
  private final RecuperacaoServico servico;
  private final AutenticacaoServico auth;

  public record Solicitar(@NotBlank @Email @Size(max = 150) String email) {}

  public record Redefinir(
      @NotBlank @Size(max = 200) String token, @NotBlank @Size(min = 10, max = 72) String senha) {}

  @PostMapping("/api/password-reset/request")
  public Object pedir(@Valid @RequestBody Solicitar d) {
    servico.solicitar(d.email());
    return Map.of("message", "Se o e-mail estiver cadastrado, você receberá as instruções.");
  }

  @PostMapping("/api/password-reset/confirm")
  public Object confirmar(@Valid @RequestBody Redefinir d) {
    servico.redefinir(d.token(), d.senha());
    return Map.of("message", "Senha alterada. Entre novamente.");
  }

  @PostMapping("/api/admin/usuarios/{id}/redefinicao")
  public Object gerar(@PathVariable String id, HttpServletRequest req) {
    return Map.of("url", servico.gerar(id, auth.admin(req)));
  }
}
