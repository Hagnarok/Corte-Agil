package br.edu.unifio.corteagil_backend.servicos;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventosServico {
  private final CopyOnWriteArraySet<SseEmitter> clients = new CopyOnWriteArraySet<>();

  public SseEmitter connect(Object snapshot) {
    var client = new SseEmitter(0L);
    clients.add(client);
    client.onCompletion(() -> clients.remove(client));
    client.onTimeout(() -> clients.remove(client));
    client.onError(e -> clients.remove(client));
    send(client, SseEmitter.event().data(snapshot));
    return client;
  }

  private void send(SseEmitter client, SseEmitter.SseEventBuilder event) {
    try {
      client.send(event);
    } catch (IOException | IllegalStateException e) {
      clients.remove(client);
      client.complete();
    }
  }

  public void broadcast(Object snapshot) {
    for (var client : clients) send(client, SseEmitter.event().data(snapshot));
  }

  @Scheduled(fixedRate = 15000)
  public void heartbeat() {
    for (var client : clients) send(client, SseEmitter.event().comment("heartbeat"));
  }

  @jakarta.annotation.PreDestroy
  public void close() {
    for (var client : clients) client.complete();
    clients.clear();
  }
}
