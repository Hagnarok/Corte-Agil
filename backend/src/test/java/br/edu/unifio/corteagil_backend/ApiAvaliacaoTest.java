package br.edu.unifio.corteagil_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.unifio.corteagil_backend.entidades.EstadoFila;
import br.edu.unifio.corteagil_backend.repositorios.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:basic-test;DB_CLOSE_DELAY=-1",
      "ADMIN_PASSWORD=TestOnly-StrongPass123"
    })
@ActiveProfiles("avaliacao")
@AutoConfigureMockMvc
class ApiAvaliacaoTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired SenhaRepositorio senhas;
  @Autowired AuditoriaRepositorio audit;
  @Autowired FilaRepositorio filas;

  @BeforeEach
  void limpar() {
    senhas.deleteAll();
    audit.deleteAll();
    filas.deleteAll();
    filas.save(new EstadoFila());
  }

  String basic(String nome, String senha) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((nome + ":" + senha).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  String admin() {
    return basic("avaliador", "Avaliacao-Admin123");
  }

  String atendente() {
    return basic("operador", "Avaliacao-Atendente123");
  }

  JsonNode criar(String tipo, Map<String, Object> data) throws Exception {
    return mapper.readTree(
        mvc.perform(
                post("/api/admin/" + tipo)
                    .header("Authorization", admin())
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(data)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void distingue401De403EAutenticaDoisPerfis() throws Exception {
    mvc.perform(get("/api/admin/servicos"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Basic realm=\"CorteAgil\""));
    mvc.perform(get("/api/admin/servicos").header("Authorization", basic("avaliador", "errada")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/servicos").header("Authorization", atendente()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/session").header("Authorization", admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("ADMIN"));
    mvc.perform(get("/api/session").header("Authorization", atendente()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("ATENDENTE"));
  }

  @Test
  void categoriaServicoCrudEIntegridade() throws Exception {
    String c =
        criar("categorias", Map.of("nome", "Carnes " + UUID.randomUUID(), "ativo", true))
            .path("id")
            .asText();
    mvc.perform(get("/api/admin/categorias/" + c).header("Authorization", admin()))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/admin/categorias/" + c)
                .header("Authorization", admin())
                .contentType("application/json")
                .content("{\"nome\":\"Preparos especiais\",\"ativo\":true}"))
        .andExpect(status().isOk());
    String s =
        criar(
                "servicos",
                Map.of("nome", "Corte " + UUID.randomUUID(), "ativo", true, "categoriaId", c))
            .path("id")
            .asText();
    mvc.perform(
            get("/api/consultas/servicos-por-categoria/" + c).header("Authorization", atendente()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(s));
    mvc.perform(delete("/api/admin/categorias/" + c).header("Authorization", admin()))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/admin/servicos/" + s).header("Authorization", admin()))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/admin/categorias/" + c).header("Authorization", admin()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/admin/categorias/" + c).header("Authorization", admin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void consultaPorSituacaoEFluxoDeFilaComBasic() throws Exception {
    mvc.perform(
            post("/api/tickets")
                .contentType("application/json")
                .content("{\"service\":\"Moído\",\"origin\":\"app\"}"))
        .andExpect(status().isCreated());
    String id = senhas.findAll().get(0).getId();
    mvc.perform(get("/api/gestao/senhas/" + id).header("Authorization", atendente()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").doesNotExist());
    mvc.perform(
            get("/api/consultas/senhas-por-situacao")
                .param("status", "waiting")
                .header("Authorization", atendente()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(delete("/api/gestao/senhas/" + id).header("Authorization", atendente()))
        .andExpect(status().isForbidden());
    for (String a : List.of("call", "start", "finish"))
      mvc.perform(post("/api/queue/" + a).header("Authorization", atendente()))
          .andExpect(status().isOk());
    mvc.perform(
            get("/api/consultas/senhas-por-situacao")
                .param("status", "done")
                .header("Authorization", atendente()))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(delete("/api/gestao/senhas/" + id).header("Authorization", admin()))
        .andExpect(status().isConflict());
    assertThat(audit.findAll())
        .anySatisfy(
            e -> {
              assertThat(e.getAction()).isEqualTo("Atendimento concluído");
              assertThat(e.getResponsavelNome()).isEqualTo("operador");
            });
  }

  @Test
  void validacoesERecursosInexistentes() throws Exception {
    mvc.perform(
            post("/api/admin/categorias")
                .header("Authorization", admin())
                .contentType("application/json")
                .content("{\"nome\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/usuarios/inexistente").header("Authorization", admin()))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/consultas/servicos-por-categoria/inexistente")
                .header("Authorization", atendente()))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/consultas/senhas-por-situacao")
                .param("status", "invalido")
                .header("Authorization", atendente()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/servicos")
                .header("Authorization", admin())
                .contentType("application/json")
                .content("{\"nome\":\"Valido\",\"ativo\":true,\"categoriaId\":\"inexistente\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void parametroObrigatorioAusenteETipoInvalidoRetornam400() throws Exception {
    mvc.perform(get("/api/consultas/senhas-por-situacao").header("Authorization", atendente()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")))
        .andExpect(jsonPath("$.errors.status").exists());
    mvc.perform(get("/api/admin/servicos").param("page", "texto").header("Authorization", admin()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("page")))
        .andExpect(jsonPath("$.errors.page").exists());
  }

  @Test
  void formatoDeConteudoIncompativelRetorna415() throws Exception {
    mvc.perform(
            post("/api/admin/categorias")
                .header("Authorization", admin())
                .contentType("text/plain")
                .content("{\"nome\":\"Cortes\",\"ativo\":true}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("application/json")));
  }

  @Test
  void validacaoIdentificaCamposSemExporValoresEnviados() throws Exception {
    String senha = "Privada-" + "x".repeat(73);
    String resposta =
        mvc.perform(
                post("/api/admin/usuarios")
                    .header("Authorization", admin())
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "nome",
                                " ",
                                "email",
                                "email-invalido",
                                "senha",
                                senha,
                                "perfil",
                                "OUTRO",
                                "ativo",
                                true))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.nome").value("Informe o nome."))
            .andExpect(jsonPath("$.errors.email").value("Informe um e-mail válido."))
            .andExpect(jsonPath("$.errors.senha").exists())
            .andExpect(jsonPath("$.errors.perfil").value("O perfil deve ser ADMIN ou ATENDENTE."))
            .andExpect(
                jsonPath("$.message")
                    .value(org.hamcrest.Matchers.containsString("Informe o nome.")))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(resposta).doesNotContain(senha, "email-invalido", "OUTRO");
  }

  @Test
  void corpoAusenteOuJsonMalformadoRetornam400() throws Exception {
    for (String corpo : List.of("", "{"))
      mvc.perform(
              post("/api/admin/categorias")
                  .header("Authorization", admin())
                  .contentType("application/json")
                  .content(corpo))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("JSON")));
  }

  @Test
  void formatoDeRespostaIncompativelRetorna406() throws Exception {
    mvc.perform(
            get("/api/admin/servicos").header("Authorization", admin()).accept("application/xml"))
        .andExpect(status().isNotAcceptable())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Accept")));
  }
}
