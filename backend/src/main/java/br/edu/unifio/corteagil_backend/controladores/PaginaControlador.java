package br.edu.unifio.corteagil_backend.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaControlador {
  @GetMapping({"/", "/entrar", "/funcionario", "/cliente", "/painel", "/totem"})
  public String page() {
    return "forward:/index.html";
  }
}
