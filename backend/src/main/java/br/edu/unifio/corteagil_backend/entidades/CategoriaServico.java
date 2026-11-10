package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CategoriaServico {
  @Id private String id;

  @Column(nullable = false, unique = true)
  private String nome;

  private boolean ativo = true;
}
