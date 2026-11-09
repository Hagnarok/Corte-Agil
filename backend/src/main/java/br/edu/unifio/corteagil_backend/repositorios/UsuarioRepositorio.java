package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {
  java.util.Optional<Usuario> findByUsername(String username);

  java.util.Optional<Usuario> findByEmail(String email);
}
