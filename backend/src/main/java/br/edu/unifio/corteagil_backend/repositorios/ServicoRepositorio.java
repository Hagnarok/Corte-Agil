package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepositorio extends JpaRepository<Servico, String> {
  java.util.Optional<Servico> findByNome(String nome);

  java.util.List<Servico> findByCategoriaIdOrderByNomeAsc(String categoriaId);
}
