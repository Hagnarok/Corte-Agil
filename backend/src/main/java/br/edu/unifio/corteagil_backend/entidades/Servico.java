package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Servico {
  @Id private String id;

  @Column(unique = true, nullable = false)
  private String nome;

  private boolean ativo = true;
}
