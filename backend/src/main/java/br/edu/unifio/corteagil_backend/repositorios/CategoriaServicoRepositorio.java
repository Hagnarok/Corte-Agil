package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.CategoriaServico;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaServicoRepositorio extends JpaRepository<CategoriaServico, String> {
  Optional<CategoriaServico> findFirstByOrderByNomeAsc();
}
