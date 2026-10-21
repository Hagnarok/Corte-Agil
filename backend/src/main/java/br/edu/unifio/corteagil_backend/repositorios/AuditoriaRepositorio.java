package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepositorio extends JpaRepository<EventoAuditoria, String> {
  java.util.List<EventoAuditoria> findTop100ByOrderByAtDesc();
}
