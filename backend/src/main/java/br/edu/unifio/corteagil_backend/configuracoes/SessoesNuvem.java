package br.edu.unifio.corteagil_backend.configuracoes;

import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@Profile("vercel")
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 28800)
public class SessoesNuvem {
  public SessoesNuvem(DataSource dataSource) {
    var jdbc = new JdbcTemplate(dataSource);
    new org.springframework.transaction.support.TransactionTemplate(
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource))
        .executeWithoutResult(
            tx -> {
              boolean postgres =
                  Boolean.TRUE.equals(
                      jdbc.execute(
                          (org.springframework.jdbc.core.ConnectionCallback<Boolean>)
                              (c ->
                                  c.getMetaData().getDatabaseProductName().equals("PostgreSQL"))));
              if (postgres) jdbc.execute("SELECT pg_advisory_xact_lock(1946)");
              jdbc.execute(
                  "CREATE TABLE IF NOT EXISTS SPRING_SESSION (PRIMARY_ID CHAR(36) NOT NULL PRIMARY KEY, SESSION_ID CHAR(36) NOT NULL, CREATION_TIME BIGINT NOT NULL, LAST_ACCESS_TIME BIGINT NOT NULL, MAX_INACTIVE_INTERVAL INTEGER NOT NULL, EXPIRY_TIME BIGINT NOT NULL, PRINCIPAL_NAME VARCHAR(100))");
              jdbc.execute(
                  "CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)");
              jdbc.execute(
                  "CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)");
              jdbc.execute(
                  "CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME)");
              jdbc.execute(
                  "CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (SESSION_PRIMARY_ID CHAR(36) NOT NULL REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE, ATTRIBUTE_NAME VARCHAR(200) NOT NULL, ATTRIBUTE_BYTES BYTEA NOT NULL, PRIMARY KEY(SESSION_PRIMARY_ID, ATTRIBUTE_NAME))");
            });
  }
}
