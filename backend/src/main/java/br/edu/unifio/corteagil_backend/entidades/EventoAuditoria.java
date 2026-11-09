package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class EventoAuditoria {
  @Id private String id;
  private String action;
  private String label;

  @ManyToOne @com.fasterxml.jackson.annotation.JsonIgnore private Usuario responsavel;
  private String responsavelNome;

  public EventoAuditoria(String id, String action, String label, long at) {
    this.id = id;
    this.action = action;
    this.label = label;
    this.at = at;
  }

  @Column(name = "occurred_at")
  private long at;
}
