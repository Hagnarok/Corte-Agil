package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.repositorios.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InicializadorBanco implements ApplicationRunner {
  private final FilaRepositorio queues;
  private final SenhaRepositorio tickets;
  private final AuditoriaRepositorio audits;
  private final ObjectMapper mapper;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    EstadoFila state = queues.findById(1L).orElseGet(() -> queues.save(new EstadoFila()));
    Path legacy = Path.of("data/legacy-queue.json");
    if (!state.isLegacyImported() && Files.exists(legacy)) {
      if (tickets.count() > 0)
        throw new IllegalStateException(
            "Importação cancelada: o banco de destino já contém senhas.");
      JsonNode json = mapper.readTree(legacy.toFile());
      if (!json.has("tickets") || !json.has("sequence"))
        throw new IllegalStateException("Arquivo de migração inválido.");
      state.setSequence(json.path("sequence").asLong());
      state.setPaused(json.path("paused").asBoolean());
      for (JsonNode item : json.path("tickets")) {
        Senha t = mapper.treeToValue(item, Senha.class);
        tickets.save(t);
        state.setNextOrder(Math.max(state.getNextOrder(), t.getOrder()));
      }
      for (JsonNode item : json.path("audit"))
        audits.save(mapper.treeToValue(item, EventoAuditoria.class));
      state.setLegacyImported(true);
    }
  }
}
