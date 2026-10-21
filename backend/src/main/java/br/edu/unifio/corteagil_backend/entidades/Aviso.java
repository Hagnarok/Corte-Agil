package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aviso {
  private String id;
  private String kind;
  private String text;

  @Column(name = "occurred_at")
  private long at;
}
