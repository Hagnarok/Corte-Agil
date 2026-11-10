package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
public class ConsultaControlador {
  private final ConsultaServico consultas;
  private final AutenticacaoServico auth;

  @GetMapping("/servicos-por-categoria/{id}")
  public Object servicos(@PathVariable String id, HttpServletRequest req) {
    auth.require(req);
    return consultas.servicosDaCategoria(id);
  }

  @GetMapping("/senhas-por-situacao")
  public Object senhas(@RequestParam String status, HttpServletRequest req) {
    auth.require(req);
    return consultas.senhasPorSituacao(status);
  }
}
