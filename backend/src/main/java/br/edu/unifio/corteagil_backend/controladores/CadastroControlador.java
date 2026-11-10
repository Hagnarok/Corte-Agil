package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.dto.CadastroRequest;
import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CadastroControlador {
  private final CadastroServico cadastros;
  private final AutenticacaoServico auth;

  @GetMapping("/api/catalogo")
  public Object catalogo() {
    return cadastros.catalogo();
  }

  @GetMapping("/api/admin/{tipo}")
  public Object lista(
      @PathVariable String tipo,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String ativo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      HttpServletRequest req) {
    auth.admin(req);
    return cadastros.pesquisar(tipo, q, ativo, page, size);
  }

  @GetMapping("/api/admin/{tipo}/{id}")
  public Object buscar(@PathVariable String tipo, @PathVariable String id, HttpServletRequest req) {
    auth.admin(req);
    return cadastros.buscar(tipo, id);
  }

  @PostMapping("/api/admin/{tipo}")
  @ResponseStatus(HttpStatus.CREATED)
  public Object criar(
      @PathVariable String tipo, @Valid @RequestBody CadastroRequest d, HttpServletRequest req) {
    return cadastros.salvar(tipo, null, d, auth.admin(req));
  }

  @PutMapping("/api/admin/{tipo}/{id}")
  public Object editar(
      @PathVariable String tipo,
      @PathVariable String id,
      @Valid @RequestBody CadastroRequest d,
      HttpServletRequest req) {
    return cadastros.salvar(tipo, id, d, auth.admin(req));
  }

  @DeleteMapping("/api/admin/{tipo}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable String tipo, @PathVariable String id, HttpServletRequest req) {
    cadastros.excluir(tipo, id, auth.admin(req));
  }
}
