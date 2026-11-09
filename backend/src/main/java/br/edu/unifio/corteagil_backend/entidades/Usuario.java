package br.edu.unifio.corteagil_backend.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
  @Id private String id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(unique = true)
  private String email;

  @JsonIgnore private String passwordHash;
  private String perfil;
  private boolean ativo = true;
  @JsonIgnore private long versaoSessao;
}
