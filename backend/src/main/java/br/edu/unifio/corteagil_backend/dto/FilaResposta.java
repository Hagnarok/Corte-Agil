package br.edu.unifio.corteagil_backend.dto;

import java.util.List;

public record FilaResposta(
    SenhaResposta current,
    List<SenhaResposta> waiting,
    boolean paused,
    Stats stats,
    long serverTime) {
  public record Stats(long completed, long absent, long averageMinutes, boolean estimated) {}
}
