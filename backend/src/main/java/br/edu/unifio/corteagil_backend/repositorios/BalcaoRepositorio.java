package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.Balcao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalcaoRepositorio extends JpaRepository<Balcao, String> {}
