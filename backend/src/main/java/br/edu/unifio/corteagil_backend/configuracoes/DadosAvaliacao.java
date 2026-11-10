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
    criar("avaliador", "Avaliacao-Admin123", "ADMIN");
    criar("operador", "Avaliacao-Atendente123", "ATENDENTE");
  }

  private void criar(String nome, String senha, String perfil) {
    if (usuarios.findByUsername(nome).isPresent()) return;
    var u = new Usuario();
    u.setId(java.util.UUID.randomUUID().toString());
    u.setUsername(nome);
    u.setPerfil(perfil);
    u.setPasswordHash(encoder.encode(senha));
    usuarios.save(u);
  }
}
