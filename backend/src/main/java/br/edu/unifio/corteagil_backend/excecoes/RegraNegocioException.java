package br.edu.unifio.corteagil_backend.excecoes;

import lombok.Getter;

@Getter
public class RegraNegocioException extends RuntimeException {
  private final int status;

  public RegraNegocioException(String message, int status) {
    super(message);
    this.status = status;
  }
}
