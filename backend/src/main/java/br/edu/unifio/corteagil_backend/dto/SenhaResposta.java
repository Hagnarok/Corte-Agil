package br.edu.unifio.corteagil_backend.dto;

import br.edu.unifio.corteagil_backend.entidades.Senha;

public record SenhaResposta(
    String id,
    String label,
    String service,
    String origin,
    String status,
    long createdAt,
    long order,
    int returns,
    Long calledAt,
    Long deadline,
    Long startedAt,
    Long finishedAt) {
  public static SenhaResposta of(Senha t) {
    return t == null
        ? null
        : new SenhaResposta(
            t.getId(),
            t.getLabel(),
            t.getService(),
            t.getOrigin(),
            t.getStatus(),
            t.getCreatedAt(),
            t.getOrder(),
            t.getReturns(),
            t.getCalledAt(),
            t.getDeadline(),
            t.getStartedAt(),
            t.getFinishedAt());
  }
}
