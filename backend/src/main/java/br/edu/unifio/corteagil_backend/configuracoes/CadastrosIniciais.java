package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.repositorios.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@RequiredArgsConstructor
public class CadastrosIniciais implements ApplicationRunner {
  private final UsuarioRepositorio usuarios;
  private final ServicoRepositorio servicos;
  private final BalcaoRepositorio balcoes;
  private final Environment env;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (usuarios.count() == 0) {
      String senha = env.getProperty("ADMIN_PASSWORD", "");
      if (senha.length() < 10
          || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalStateException(
            "Configure ADMIN_PASSWORD com pelo menos 10 caracteres e até 72 bytes.");
      var u = new Usuario();
      u.setId(UUID.randomUUID().toString());
      u.setUsername("admin");
      u.setEmail(
          env.getProperty("ADMIN_EMAIL") == null
              ? null
              : env.getProperty("ADMIN_EMAIL").trim().toLowerCase());
      u.setPerfil("ADMIN");
      u.setPasswordHash(new BCryptPasswordEncoder().encode(senha));
      usuarios.save(u);
    }
    if (servicos.count() == 0)
      for (String nome :
          List.of("Não informado", "Bife rápido", "Peça inteira", "Moído", "Outros cortes")) {
        var s = new Servico();
        s.setId(UUID.randomUUID().toString());
        s.setNome(nome);
        servicos.save(s);
      }
    if (balcoes.count() == 0) {
      var b = new Balcao();
      b.setId(UUID.randomUUID().toString());
      b.setNome("Balcão 01");
      balcoes.save(b);
    }
  }
}
