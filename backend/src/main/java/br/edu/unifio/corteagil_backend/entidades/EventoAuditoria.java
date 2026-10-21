package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoAuditoria {
  @Id private String id;
  private String action;
  private String label;

  @Column(name = "occurred_at")
  private long at;
}
