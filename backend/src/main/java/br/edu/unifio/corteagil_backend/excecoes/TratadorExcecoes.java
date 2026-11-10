package br.edu.unifio.corteagil_backend.excecoes;

import java.util.Map;
import java.util.TreeMap;
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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
    var campos = new TreeMap<String, String>();
    for (var erro : e.getBindingResult().getFieldErrors())
      campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
    String mensagem =
        campos.isEmpty()
            ? "Dados inválidos. Confira os campos enviados."
            : "Dados inválidos. " + String.join(" ", campos.values());
    return ResponseEntity.badRequest().body(Map.of("message", mensagem, "errors", campos));
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  ResponseEntity<?> jsonInvalido(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Corpo ausente ou JSON inválido. Envie um objeto JSON válido."));
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  ResponseEntity<?> parametroAusente(
      org.springframework.web.bind.MissingServletRequestParameterException e) {
    String mensagem = "Informe o parâmetro obrigatório '" + e.getParameterName() + "'.";
    return ResponseEntity.badRequest()
        .body(Map.of("message", mensagem, "errors", Map.of(e.getParameterName(), mensagem)));
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<?> formatoIncompativel(Exception e) {
    return ResponseEntity.status(415)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "message",
                "Formato de conteúdo não suportado. Envie JSON com Content-Type: application/json."));
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
  ResponseEntity<?> respostaIncompativel(Exception e) {
    return ResponseEntity.status(406)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "message",
                "Formato de resposta não disponível. Utilize Accept: application/json."));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  ResponseEntity<?> conflito(Exception e) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message",
                "Registro duplicado ou vinculado a um histórico. Desative-o em vez de excluir."));
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  ResponseEntity<?> tipoDoParametro(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
    String mensagem = "O parâmetro '" + e.getName() + "' possui um valor inválido.";
    return ResponseEntity.badRequest()
        .body(Map.of("message", mensagem, "errors", Map.of(e.getName(), mensagem)));
  }

  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
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
