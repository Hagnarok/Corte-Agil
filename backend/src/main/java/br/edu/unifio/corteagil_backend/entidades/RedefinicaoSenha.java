package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RedefinicaoSenha {
  @Id private String hash;

  @ManyToOne(optional = false)
  private Usuario usuario;

  private long expiraEm;
  private boolean usado;
}
