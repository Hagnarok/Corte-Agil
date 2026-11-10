package br.edu.unifio.corteagil_backend;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unifio.corteagil_backend.configuracoes.SessoesNuvem;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

@SpringJUnitConfig(SessoesNuvemTest.Config.class)
@ActiveProfiles("vercel")
class SessoesNuvemTest {
  @Configuration
  @Import(SessoesNuvem.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          "jdbc:h2:mem:sessoes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource ds) {
      return new DataSourceTransactionManager(ds);
    }
  }

  @Autowired JdbcIndexedSessionRepository repo;

  @Test
  void persisteSessaoComAtributosEPodeEncerrar() {
    org.springframework.session.SessionRepository<org.springframework.session.Session> repository =
        (org.springframework.session.SessionRepository) repo;
    var session = repository.createSession();
    session.setAttribute("usuarioId", "funcionario-1");
    session.setAttribute("versao", 3L);
    repository.save(session);
    var saved = repository.findById(session.getId());
    assertThat(saved).isNotNull();
    assertThat((String) saved.getAttribute("usuarioId")).isEqualTo("funcionario-1");
    assertThat((Long) saved.getAttribute("versao")).isEqualTo(3L);
    repository.deleteById(session.getId());
    assertThat(repository.findById(session.getId())).isNull();
  }
}
