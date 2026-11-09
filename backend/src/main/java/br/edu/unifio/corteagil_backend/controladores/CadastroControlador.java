package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.dto.*;
import br.edu.unifio.corteagil_backend.repositorios.*;
import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CadastroControlador {
  private final CadastroServico cadastros;
  private final AutenticacaoServico auth;
  private final ServicoRepositorio servicos;
  private final BalcaoRepositorio balcoes;

  @GetMapping("/api/catalogo")
  public Object catalogo() {
    return Map.of(
        "servicos",
        servicos.findAll().stream().filter(s -> s.isAtivo()).toList(),
        "balcoes",
        balcoes.findAll().stream().filter(b -> b.isAtivo()).toList());
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
    if (page < 0
        || size < 1
        || size > 50
        || q.length() > 100
        || !List.of("", "true", "false").contains(ativo))
      throw new br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException(
          "Paginação ou pesquisa inválida.", 400);
    var all =
        cadastros.listar(tipo).stream()
            .filter(
                x ->
                    ativo.isBlank()
                        || Boolean.toString(
                                x instanceof br.edu.unifio.corteagil_backend.entidades.Usuario u
                                    ? u.isAtivo()
                                    : x
                                            instanceof
                                            br.edu.unifio.corteagil_backend.entidades.Servico s
                                        ? s.isAtivo()
                                        : ((br.edu.unifio.corteagil_backend.entidades.Balcao) x)
                                            .isAtivo())
                            .equals(ativo))
            .filter(
                x -> {
                  String nome =
                      x instanceof br.edu.unifio.corteagil_backend.entidades.Usuario u
                          ? u.getUsername()
                          : x instanceof br.edu.unifio.corteagil_backend.entidades.Servico s
                              ? s.getNome()
                              : ((br.edu.unifio.corteagil_backend.entidades.Balcao) x).getNome();
                  return nome.toLowerCase().contains(q.toLowerCase());
                })
            .sorted(
                java.util.Comparator.comparing(
                    x ->
                        x instanceof br.edu.unifio.corteagil_backend.entidades.Usuario u
                            ? u.getUsername()
                            : x instanceof br.edu.unifio.corteagil_backend.entidades.Servico s
                                ? s.getNome()
                                : ((br.edu.unifio.corteagil_backend.entidades.Balcao) x).getNome()))
            .toList();
    return Map.of(
        "items", all.stream().skip((long) page * size).limit(size).toList(), "total", all.size());
  }

  @PostMapping("/api/admin/{tipo}")
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
  public Object excluir(
      @PathVariable String tipo, @PathVariable String id, HttpServletRequest req) {
    cadastros.excluir(tipo, id, auth.admin(req));
    return Map.of("ok", true);
  }
}
