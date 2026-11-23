package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.entidades.Usuario;
import br.edu.unifio.corteagil_backend.repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("avaliacao")
@Order(40)
@RequiredArgsConstructor
public class DadosAvaliacao implements ApplicationRunner {
  private final UsuarioRepositorio usuarios;
  private final PasswordEncoder encoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    criar("admin", "admin@piunifio", "ADMIN", "Avaliacao-Admin123");
    criar("professor.pi", "Professor@piunifio", "ADMIN", null);
    criar("avaliador", "avaliador@piunifio", "ADMIN", "Avaliacao-Admin123");
    criar("operador", "operador@piunifio", "ATENDENTE", "Avaliacao-Atendente123");
  }

  private void criar(String nome, String senha, String perfil, String senhaAntiga) {
    var existente = usuarios.findByUsername(nome);
    if (existente.isPresent()) {
      var u = existente.get();
      if (senhaAntiga != null && encoder.matches(senhaAntiga, u.getPasswordHash())) {
        u.setPasswordHash(encoder.encode(senha));
        u.setVersaoSessao(u.getVersaoSessao() + 1);
        usuarios.save(u);
      }
      return;
    }
    var u = new Usuario();
    u.setId(java.util.UUID.randomUUID().toString());
    u.setUsername(nome);
    u.setPerfil(perfil);
    u.setPasswordHash(encoder.encode(senha));
    usuarios.save(u);
  }
}
