package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.Senha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SenhaRepositorio extends JpaRepository<Senha, String> {}
