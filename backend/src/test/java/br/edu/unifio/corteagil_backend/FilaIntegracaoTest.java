package br.edu.unifio.corteagil_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.unifio.corteagil_backend.dto.EmissaoRequest;
import br.edu.unifio.corteagil_backend.entidades.EstadoFila;
import br.edu.unifio.corteagil_backend.repositorios.*;
import br.edu.unifio.corteagil_backend.servicos.FilaServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:queue-test;DB_CLOSE_DELAY=-1",
      "ADMIN_PASSWORD=TestOnly-StrongPass123",
      "CODESPACE_NAME=test-space",
      "ALLOWED_ORIGINS=https://test.example.com"
    })
@AutoConfigureMockMvc
class FilaIntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired FilaServico queue;
  @Autowired SenhaRepositorio tickets;
  @Autowired FilaRepositorio queues;
  @Autowired AuditoriaRepositorio audits;

  @BeforeEach
  void clear() {
    tickets.deleteAll();
    audits.deleteAll();
    queues.deleteAll();
    queues.save(new EstadoFila());
  }

  MockHttpSession login() throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/login")
                    .contentType("application/json")
                    .content("{\"username\":\"admin\",\"password\":\"TestOnly-StrongPass123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession();
  }

  @Test
  void authenticationAndProxyOrigins() throws Exception {
    for (String origin :
        List.of(
            "https://localhost:3000",
            "https://test-space-3000.app.github.dev",
            "https://test.example.com")) {
      mvc.perform(post("/api/logout").header("Origin", origin))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(post("/api/logout").header("Origin", "https://evil.invalid"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/queue/call")).andExpect(status().isUnauthorized());
    var session = login();
    mvc.perform(get("/api/session").session(session)).andExpect(status().isOk());
    mvc.perform(post("/api/logout").session(session)).andExpect(status().isOk());
    mvc.perform(get("/api/session")).andExpect(status().isUnauthorized());
  }

  @Test
  void fifoLifecycleAndPublicPrivacy() throws Exception {
    var first = queue.issue(new EmissaoRequest("Moído", "app"));
    var second = queue.issue(new EmissaoRequest("Bife rápido", "totem"));
    assertThat(first.get("label")).isEqualTo("A001");
    mvc.perform(get("/api/state"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.waiting[0].token").doesNotExist())
        .andExpect(jsonPath("$.waiting[0].notifications").doesNotExist());
    var session = login();
    mvc.perform(post("/api/queue/call").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value(first.get("label")));
    mvc.perform(post("/api/queue/call").session(session)).andExpect(status().isConflict());
    mvc.perform(post("/api/queue/finish").session(session)).andExpect(status().isConflict());
    mvc.perform(post("/api/queue/absent").session(session)).andExpect(status().isConflict());
    for (String action : List.of("start", "finish"))
      mvc.perform(post("/api/queue/" + action).session(session)).andExpect(status().isOk());
    mvc.perform(get("/api/tickets/" + first.get("token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("done"));
    assertThat(queue.snapshot().stats().completed()).isEqualTo(1);
    assertThat(queue.snapshot().waiting().get(0).label()).isEqualTo(second.get("label"));
  }

  @Test
  void absenceReturnsToTailWithSameNumber() {
    var first = queue.issue(new EmissaoRequest("Moído", "app"));
    queue.issue(new EmissaoRequest("Moído", "totem"));
    var called = queue.action("call");
    var t = tickets.findById(called.id()).orElseThrow();
    t.setDeadline(System.currentTimeMillis() - 1);
    tickets.save(t);
    queue.action("absent");
    queue.rejoin(first.get("token"));
    assertThat(queue.snapshot().waiting())
        .extracting(v -> v.label())
        .containsExactly("A002", "A001");
    assertThat(queue.ticket(first.get("token")).get("returns")).isEqualTo(1);
    assertThat(queue.ticket(first.get("token")).get("notifications")).isNotNull();
  }

  @Test
  void pausedAndValidation() throws Exception {
    queue.action("pause");
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content("{\"service\":\"Moído\",\"origin\":\"app\"}"))
        .andExpect(status().isConflict());
    queue.action("pause");
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content("{\"service\":\"Inválido\",\"origin\":\"app\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/tickets/missing")).andExpect(status().isNotFound());
  }

  @Test
  void concurrentIssuanceHasUniqueSequence() throws Exception {
    var pool = Executors.newFixedThreadPool(6);
    try {
      List<Callable<String>> work = new ArrayList<>();
      for (int i = 0; i < 20; i++)
        work.add(() -> queue.issue(new EmissaoRequest("Moído", "app")).get("label"));
      var labels = new HashSet<String>();
      for (var result : pool.invokeAll(work)) labels.add(result.get());
      assertThat(labels).hasSize(20);
      assertThat(tickets.count()).isEqualTo(20);
    } finally {
      pool.shutdownNow();
    }
  }
}
