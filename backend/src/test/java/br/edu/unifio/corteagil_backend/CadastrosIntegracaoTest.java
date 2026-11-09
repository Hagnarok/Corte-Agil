package br.edu.unifio.corteagil_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.repositorios.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:cadastros-test;DB_CLOSE_DELAY=-1",
      "ADMIN_PASSWORD=TestOnly-StrongPass123",
      "PUBLIC_BASE_URL=https://test.example.com",
      "MAIL_FROM=teste@example.com"
    })
@AutoConfigureMockMvc
class CadastrosIntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepositorio usuarios;
  @Autowired SenhaRepositorio senhas;
  @Autowired AuditoriaRepositorio auditoria;
  @Autowired RedefinicaoRepositorio redefinicoes;
  @Autowired FilaRepositorio filas;
  @MockitoBean JavaMailSender mail;
  MockHttpSession admin;

  @BeforeEach
  void preparar() throws Exception {
    senhas.deleteAll();
    auditoria.deleteAll();
    redefinicoes.deleteAll();
    usuarios.deleteAll();
    filas.deleteAll();
    filas.save(new EstadoFila());
    var u = new Usuario();
    u.setId(UUID.randomUUID().toString());
    u.setUsername("admin");
    u.setPerfil("ADMIN");
    u.setEmail("admin@example.com");
    u.setPasswordHash(new BCryptPasswordEncoder().encode("TestOnly-StrongPass123"));
    usuarios.save(u);
    admin = login("admin", "TestOnly-StrongPass123");
  }

  MockHttpSession login(String username, String password) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/login")
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession();
  }

  JsonNode json(MvcResult r) throws Exception {
    return mapper.readTree(r.getResponse().getContentAsString());
  }

  JsonNode criar(String tipo, Map<String, Object> d) throws Exception {
    return json(
        mvc.perform(
                post("/api/admin/" + tipo)
                    .session(admin)
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(d)))
            .andExpect(status().isOk())
            .andReturn());
  }

  Map<String, Object> usuario(String nome) {
    return Map.of(
        "nome",
        nome,
        "email",
        nome + "@example.com",
        "senha",
        "Atendente-Test123",
        "perfil",
        "ATENDENTE",
        "ativo",
        true);
  }

  String gerarLink(String id) throws Exception {
    return json(mvc.perform(post("/api/admin/usuarios/" + id + "/redefinicao").session(admin))
            .andExpect(status().isOk())
            .andReturn())
        .path("url")
        .asText()
        .split("#token=")[1];
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder confirmar(
      String token) {
    return post("/api/password-reset/confirm")
        .contentType("application/json")
        .content("{\"token\":\"" + token + "\",\"senha\":\"NovaSenha-Test123\"}");
  }

  @Test
  void perfisEUsuarioCrud() throws Exception {
    var u = criar("usuarios", usuario("operador"));
    String id = u.path("id").asText();
    assertThat(u.has("passwordHash")).isFalse();
    assertThat(u.has("versaoSessao")).isFalse();
    var atendente = login("operador", "Atendente-Test123");
    mvc.perform(get("/api/session").session(atendente))
        .andExpect(jsonPath("$.perfil").value("ATENDENTE"));
    mvc.perform(get("/api/admin/usuarios").session(atendente)).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/servicos")
                .session(atendente)
                .contentType("application/json")
                .content("{\"nome\":\"Proibido\",\"ativo\":true}"))
        .andExpect(status().isForbidden());
    var alterado = new HashMap<>(usuario("operador"));
    alterado.put("ativo", false);
    alterado.put("senha", "");
    mvc.perform(
            put("/api/admin/usuarios/" + id)
                .session(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(alterado)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/session").session(atendente)).andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/admin/usuarios/" + id).session(admin)).andExpect(status().isOk());
    String adminId = usuarios.findByUsername("admin").orElseThrow().getId();
    mvc.perform(delete("/api/admin/usuarios/" + adminId).session(admin))
        .andExpect(status().isConflict());
  }

  @Test
  void catalogoPesquisaPaginacaoEVinculos() throws Exception {
    String nome = "Especial " + UUID.randomUUID();
    var s = criar("servicos", Map.of("nome", nome, "ativo", true));
    String id = s.path("id").asText();
    criar("servicos", Map.of("nome", nome + " 2", "ativo", true));
    mvc.perform(
            get("/api/admin/servicos")
                .session(admin)
                .param("q", nome)
                .param("size", "1")
                .param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.total").value(2));
    mvc.perform(get("/api/admin/servicos").session(admin).param("size", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/servicos")
                .session(admin)
                .contentType("application/json")
                .content("{\"nome\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/admin/servicos/" + id)
                .session(admin)
                .header("Origin", "https://evil.invalid")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("nome", nome, "ativo", false))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("service", nome, "origin", "app"))))
        .andExpect(status().isCreated());
    mvc.perform(delete("/api/admin/servicos/" + id).session(admin))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/admin/servicos/" + id)
                .session(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("nome", nome, "ativo", false))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("service", nome, "origin", "app"))))
        .andExpect(status().isBadRequest());
    var b = criar("balcoes", Map.of("nome", "Extra " + UUID.randomUUID(), "ativo", true));
    String bid = b.path("id").asText();
    mvc.perform(
            put("/api/admin/balcoes/" + bid)
                .session(admin)
                .contentType("application/json")
                .content("{\"nome\":\"Balcão extra editado\",\"ativo\":false}"))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/admin/balcoes/" + bid).session(admin)).andExpect(status().isOk());
    mvc.perform(delete("/api/admin/balcoes/inexistente").session(admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void senhaCrudPreservaHistoricoENumeracao() throws Exception {
    var emitida =
        json(
            mvc.perform(
                    post("/api/tickets")
                        .contentType("application/json")
                        .content("{\"service\":\"Moído\",\"origin\":\"app\"}"))
                .andExpect(status().isCreated())
                .andReturn());
    var s = senhas.findAll().get(0);
    assertThat(s.getServico()).isNotNull();
    assertThat(s.getBalcao()).isNotNull();
    mvc.perform(
            put("/api/gestao/senhas/" + s.getId())
                .session(admin)
                .contentType("application/json")
                .content("{\"service\":\"Peça inteira\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service").value("Peça inteira"));
    mvc.perform(
            get("/api/gestao/senhas").session(admin).param("q", "Peça").param("status", "waiting"))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].token").doesNotExist());
    mvc.perform(delete("/api/gestao/senhas/" + s.getId()).session(admin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/tickets/" + emitida.path("token").asText()))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content("{\"service\":\"Moído\",\"origin\":\"totem\"}"))
        .andExpect(jsonPath("$.label").value("A002"));
    mvc.perform(post("/api/queue/call").session(admin)).andExpect(status().isOk());
    s = senhas.findAll().get(0);
    mvc.perform(delete("/api/gestao/senhas/" + s.getId()).session(admin))
        .andExpect(status().isConflict());
    assertThat(auditoria.findAll())
        .anySatisfy(
            e -> {
              assertThat(e.getAction()).isEqualTo("Senha chamada");
              assertThat(e.getResponsavelNome()).isEqualTo("admin");
              assertThat(e.getAt()).isPositive();
            });
    assertThat(s.getAtendente()).isNotNull();
  }

  @Test
  void recuperacaoPorEmailUsoUnicoESessaoRevogada() throws Exception {
    var u = criar("usuarios", usuario("recuperar"));
    var sess = login("recuperar", "Atendente-Test123");
    mvc.perform(
            post("/api/password-reset/request")
                .contentType("application/json")
                .content("{\"email\":\"recuperar@example.com\"}"))
        .andExpect(status().isOk());
    var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mail).send(captor.capture());
    String texto = captor.getValue().getText();
    assertThat(texto).contains("https://test.example.com/recuperar-senha#token=");
    String token = texto.split("#token=")[1];
    assertThat(redefinicoes.findAll().get(0).getHash()).doesNotContain(token);
    mvc.perform(confirmar(token)).andExpect(status().isOk());
    mvc.perform(confirmar(token)).andExpect(status().isBadRequest());
    mvc.perform(get("/api/session").session(sess)).andExpect(status().isUnauthorized());
    login("recuperar", "NovaSenha-Test123");
    String expirado = gerarLink(u.path("id").asText());
    var r = redefinicoes.findAll().stream().filter(x -> !x.isUsado()).findFirst().orElseThrow();
    r.setExpiraEm(1);
    redefinicoes.save(r);
    mvc.perform(confirmar(expirado)).andExpect(status().isBadRequest());
  }

  @Test
  void redefinicaoConcorrenteAceitaUmUnicoUso() throws Exception {
    String id = criar("usuarios", usuario("concorrente")).path("id").asText();
    String token = gerarLink(id);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var respostas =
          pool.invokeAll(
              List.<Callable<Integer>>of(
                  () -> mvc.perform(confirmar(token)).andReturn().getResponse().getStatus(),
                  () -> mvc.perform(confirmar(token)).andReturn().getResponse().getStatus()));
      assertThat(List.of(respostas.get(0).get(), respostas.get(1).get()))
          .containsExactlyInAnyOrder(200, 400);
    } finally {
      pool.shutdownNow();
    }
  }
}
