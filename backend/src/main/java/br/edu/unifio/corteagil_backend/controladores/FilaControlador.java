package br.edu.unifio.corteagil_backend.controladores;

import br.edu.unifio.corteagil_backend.dto.*;
import br.edu.unifio.corteagil_backend.servicos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FilaControlador {
  private final FilaServico queue;
  private final EventosServico events;
  private final AutenticacaoServico auth;

  @GetMapping("/state")
  public FilaResposta state() {
    return queue.snapshot();
  }

  @GetMapping(value = "/events", produces = "text/event-stream")
  public SseEmitter events() {
    return events.connect(queue.snapshot());
  }

  @PostMapping("/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  public Object issue(@Valid @RequestBody EmissaoRequest req) {
    return queue.issue(req);
  }

  @GetMapping("/tickets/{token}")
  public Object ticket(@PathVariable String token) {
    return queue.ticket(token);
  }

  @PostMapping("/tickets/{token}/return")
  public Object rejoin(@PathVariable String token) {
    return queue.rejoin(token);
  }

  @PostMapping("/queue/{action}")
  public Object action(@PathVariable String action, HttpServletRequest req) {
    auth.require(req);
    var result = queue.action(action);
    return result == null ? Map.of("ok", true) : result;
  }

  @GetMapping("/audit")
  public Object audit(HttpServletRequest req) {
    auth.require(req);
    return queue.audit();
  }
}
