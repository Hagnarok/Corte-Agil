package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.entidades.CategoriaServico;
import br.edu.unifio.corteagil_backend.repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
@RequiredArgsConstructor
public class CategoriasIniciais implements ApplicationRunner {
  private final CategoriaServicoRepositorio categorias;
  private final ServicoRepositorio servicos;
  private final Environment env;
  private final javax.sql.DataSource ds;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (env.matchesProfiles("vercel"))
      new org.springframework.jdbc.core.JdbcTemplate(ds)
          .execute("SELECT pg_advisory_xact_lock(1947)");
    var categoria =
        categorias
            .findFirstByOrderByNomeAsc()
            .orElseGet(
                () -> {
                  var c = new CategoriaServico();
                  c.setId(java.util.UUID.randomUUID().toString());
                  c.setNome("Cortes e preparos");
                  return categorias.save(c);
                });
    for (var s : servicos.findAll()) if (s.getCategoria() == null) s.setCategoria(categoria);
  }
}
