package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "queue_state")
@Getter
@Setter
@NoArgsConstructor
public class EstadoFila {
  @Id private Long id = 1L;
  private long sequence;
  private long nextOrder;
  private boolean paused;
  private boolean legacyImported;
}
