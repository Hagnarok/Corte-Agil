package br.edu.unifio.corteagil_backend.excecoes;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class TratadorExcecoes {
  @ExceptionHandler(RegraNegocioException.class)
  ResponseEntity<?> business(RegraNegocioException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Dados inválidos. Confira os campos enviados."));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  ResponseEntity<?> conflito(Exception e) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message",
                "Registro duplicado ou vinculado a um histórico. Desative-o em vez de excluir."));
  }

  @ExceptionHandler({
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  ResponseEntity<?> parametro(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("message", "Parâmetros inválidos."));
  }

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.servlet.NoHandlerFoundException.class
  })
  ResponseEntity<?> naoEncontrado(Exception e) {
    return ResponseEntity.status(404).body(Map.of("message", "Recurso não encontrado."));
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<?> metodo(Exception e) {
    return ResponseEntity.status(405)
        .body(Map.of("message", "Método não permitido para esta operação."));
  }

  @ExceptionHandler({
    org.springframework.web.context.request.async.AsyncRequestTimeoutException.class,
    org.springframework.web.context.request.async.AsyncRequestNotUsableException.class
  })
  @ResponseStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
  void conexaoEncerrada(Exception e) {
    /* A resposta SSE pode já estar concluída. */
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> inesperado(Exception e) {
    log.error("Falha ao processar requisição", e);
    return ResponseEntity.status(500)
        .body(Map.of("message", "Não foi possível concluir a operação. Tente novamente."));
  }
}
