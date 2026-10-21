package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.EstadoFila;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilaRepositorio extends JpaRepository<EstadoFila, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select q from EstadoFila q where q.id = 1")
  EstadoFila lockState();
}
