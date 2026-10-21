package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.dto.*;
import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.repositorios.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class FilaServico {
  private final FilaRepositorio queues;
  private final SenhaRepositorio tickets;
  private final AuditoriaRepositorio audits;
  private final TransactionTemplate transactions;
  private final EventosServico events;
  private final ObjectMapper mapper;
  private static final List<String> SERVICES =
      List.of("Não informado", "Bife rápido", "Peça inteira", "Moído", "Outros cortes");
  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  private RegraNegocioException fail(String text) {
    return new RegraNegocioException(text, 409);
  }

  private List<Senha> waiting(List<Senha> all) {
    return all.stream()
        .filter(t -> t.getStatus().equals("waiting"))
        .sorted(Comparator.comparingLong(Senha::getOrder))
        .toList();
  }

  private Senha current(List<Senha> all) {
    return all.stream()
        .filter(t -> List.of("called", "serving").contains(t.getStatus()))
        .findFirst()
        .orElse(null);
  }

  private Senha own(List<Senha> all, String token) {
    return all.stream()
        .filter(t -> t.getToken().equals(token))
        .findFirst()
        .orElseThrow(() -> new RegraNegocioException("Senha não encontrada.", 404));
  }

  private void notice(Senha t, String kind, String text, long now) {
    t.getNotifications().add(new Aviso(UUID.randomUUID().toString(), kind, text, now));
    if (t.getNotifications().size() > 20) t.getNotifications().remove(0);
  }

  private void audit(String action, Senha t, long now) {
    audits.save(
        new EventoAuditoria(
            UUID.randomUUID().toString(), action, t == null ? "Fila" : t.getLabel(), now));
  }

  private void updateNotices(List<Senha> all, long now) {
    var list = waiting(all);
    int active = current(all) == null ? 0 : 1;
    for (int i = 0; i < list.size(); i++) {
      var t = list.get(i);
      int ahead = i + active;
      if ((ahead == 1 || ahead == 2) && t.getNotified().add(t.getReturns() + ":" + ahead))
        notice(
            t,
            "ahead-" + ahead,
            ahead == 2
                ? "Faltam duas senhas para a sua vez. Fique por perto."
                : "Falta uma senha para a sua vez. Volte ao açougue.",
            now);
    }
  }

  public synchronized <T> T mutate(Function<Context, T> action) {
    T result =
        transactions.execute(
            tx -> {
              EstadoFila state = queues.lockState();
              List<Senha> all = new ArrayList<>(tickets.findAll());
              T value = action.apply(new Context(state, all, System.currentTimeMillis()));
              updateNotices(all, System.currentTimeMillis());
              tickets.saveAll(all);
              return value;
            });
    events.broadcast(snapshot());
    return result;
  }

  public record Context(EstadoFila state, List<Senha> all, long now) {}

  public Map<String, String> issue(EmissaoRequest req) {
    return mutate(
        c -> {
          if (c.state.isPaused()) throw fail("A retirada de senhas está pausada.");
          if (!SERVICES.contains(req.service()))
            throw new RegraNegocioException("Serviço inválido.", 400);
          var t = new Senha();
          t.setId(UUID.randomUUID().toString());
          t.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
          c.state.setSequence(c.state.getSequence() + 1);
          t.setLabel("A" + String.format("%03d", c.state.getSequence()));
          t.setService(req.service());
          t.setOrigin(req.origin());
          t.setStatus("waiting");
          t.setCreatedAt(c.now);
          c.state.setNextOrder(c.state.getNextOrder() + 1);
          t.setOrder(c.state.getNextOrder());
          c.all.add(t);
          audit("Senha emitida", t, c.now);
          return Map.of("token", t.getToken(), "label", t.getLabel());
        });
  }

  public SenhaResposta action(String action) {
    return mutate(
        c -> {
          if (action.equals("pause")) {
            c.state.setPaused(!c.state.isPaused());
            audit(c.state.isPaused() ? "Emissão pausada" : "Emissão retomada", null, c.now);
            return null;
          }
          Senha t = current(c.all);
          if (action.equals("call")) {
            if (t != null) throw fail("Finalize o atendimento atual antes de chamar o próximo.");
            t =
                waiting(c.all).stream()
                    .findFirst()
                    .orElseThrow(() -> fail("Não há clientes aguardando."));
            t.setStatus("called");
            t.setCalledAt(c.now);
            t.setDeadline(c.now + 30000);
            notice(t, "called", "É a sua vez! Apresente-se ao balcão do açougue.", c.now);
            audit("Senha chamada", t, c.now);
          } else {
            if (t == null) throw fail("Não há atendimento ativo.");
            switch (action) {
              case "recall" -> {
                if (!t.getStatus().equals("called")) throw fail("Cliente já está em atendimento.");
                t.setCalledAt(c.now);
                t.setDeadline(c.now + 30000);
                notice(t, "called", "Sua senha está sendo chamada novamente. Vá ao balcão.", c.now);
                audit("Chamada repetida", t, c.now);
              }
              case "start" -> {
                if (!t.getStatus().equals("called")) throw fail("Atendimento já iniciado.");
                t.setStatus("serving");
                t.setStartedAt(c.now);
                audit("Atendimento iniciado", t, c.now);
              }
              case "finish" -> {
                if (!t.getStatus().equals("serving"))
                  throw fail("Confirme a chegada do cliente primeiro.");
                t.setStatus("done");
                t.setFinishedAt(c.now);
                notice(t, "done", "Atendimento concluído. Obrigado e boas compras!", c.now);
                audit("Atendimento concluído", t, c.now);
              }
              case "absent" -> {
                if (!t.getStatus().equals("called") || c.now < t.getDeadline())
                  throw fail("Aguarde os 30 segundos da chamada para registrar ausência.");
                t.setStatus("absent");
                notice(t, "absent", "Você perdeu sua vez. Deseja retornar à fila?", c.now);
                audit("Cliente ausente", t, c.now);
              }
              default -> throw new RegraNegocioException("Ação inválida.", 400);
            }
          }
          return SenhaResposta.of(t);
        });
  }

  public Map<String, Object> rejoin(String token) {
    mutate(
        c -> {
          var t = own(c.all, token);
          if (!t.getStatus().equals("absent"))
            throw fail("Somente senhas ausentes podem retornar.");
          if (c.state.isPaused()) throw fail("A fila está pausada. Tente novamente em instantes.");
          t.setStatus("waiting");
          t.setReturns(t.getReturns() + 1);
          c.state.setNextOrder(c.state.getNextOrder() + 1);
          t.setOrder(c.state.getNextOrder());
          notice(t, "returned", "Você retornou ao final da fila com a mesma senha.", c.now);
          audit("Retorno ao final da fila", t, c.now);
          return null;
        });
    return transactions.execute(tx -> ticket(token));
  }

  private FilaResposta snapshot(EstadoFila state, List<Senha> all, long now) {
    var today = Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate();
    var done =
        all.stream()
            .filter(
                t ->
                    t.getStatus().equals("done")
                        && Instant.ofEpochMilli(t.getFinishedAt())
                            .atZone(ZONE)
                            .toLocalDate()
                            .equals(today))
            .toList();
    var recent =
        done.stream()
            .filter(t -> t.getStartedAt() != null)
            .sorted(Comparator.comparing(Senha::getFinishedAt).reversed())
            .limit(20)
            .toList();
    long average =
        recent.isEmpty()
            ? 4
            : Math.max(
                1,
                Math.round(
                    recent.stream()
                            .mapToLong(t -> t.getFinishedAt() - t.getStartedAt())
                            .average()
                            .orElse(0)
                        / 60000));
    return new FilaResposta(
        SenhaResposta.of(current(all)),
        waiting(all).stream().map(SenhaResposta::of).toList(),
        state.isPaused(),
        new FilaResposta.Stats(
            done.size(),
            all.stream().filter(t -> t.getStatus().equals("absent")).count(),
            average,
            recent.isEmpty()),
        now);
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public FilaResposta snapshot() {
    return snapshot(
        queues.findById(1L).orElseThrow(), tickets.findAll(), System.currentTimeMillis());
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public Map<String, Object> ticket(String token) {
    var all = tickets.findAll();
    var t = own(all, token);
    var snap = snapshot(queues.findById(1L).orElseThrow(), all, System.currentTimeMillis());
    Map<String, Object> result =
        mapper.convertValue(
            SenhaResposta.of(t),
            new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
    int index = waiting(all).indexOf(t);
    int ahead = index < 0 ? 0 : index + (snap.current() == null ? 0 : 1);
    result.put("token", t.getToken());
    result.put("notifications", List.copyOf(t.getNotifications()));
    result.put("ahead", ahead);
    result.put("position", index < 0 ? 0 : ahead + 1);
    result.put("estimatedMinutes", ahead * snap.stats().averageMinutes());
    return result;
  }

  public List<EventoAuditoria> audit() {
    return audits.findTop100ByOrderByAtDesc();
  }
}
