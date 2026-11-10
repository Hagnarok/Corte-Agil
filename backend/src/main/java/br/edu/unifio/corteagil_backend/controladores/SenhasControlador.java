package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gestao/senhas")
@RequiredArgsConstructor
public class SenhasControlador {
  private final FilaServico fila;
  private final AutenticacaoServico auth;

  public record Editar(
      @NotBlank(message = "Informe o serviço.")
          @Size(max = 80, message = "O serviço deve ter até 80 caracteres.")
          String service) {}

  @GetMapping
  public Object listar(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      HttpServletRequest req) {
    auth.require(req);
    return fila.listarSenhas(q, status, page, size);
  }

  @GetMapping("/{id}")
  public Object buscar(@PathVariable String id, HttpServletRequest req) {
    auth.require(req);
    return fila.buscarSenha(id);
  }

  @PutMapping("/{id}")
  public Object editar(
      @PathVariable String id, @Valid @RequestBody Editar d, HttpServletRequest req) {
    auth.admin(req);
    return fila.editarSenha(id, d.service());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable String id, HttpServletRequest req) {
    auth.admin(req);
    fila.excluirSenha(id);
  }
}
