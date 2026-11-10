package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetalhesUsuarioServico implements UserDetailsService {
  private final UsuarioRepositorio usuarios;

  @Override
  public UserDetails loadUserByUsername(String nome) {
    var u =
        usuarios
            .findByUsername(nome)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha incorretos."));
    return User.withUsername(u.getUsername())
        .password(u.getPasswordHash())
        .roles(u.getPerfil())
        .disabled(!u.isAtivo())
        .build();
  }
}
