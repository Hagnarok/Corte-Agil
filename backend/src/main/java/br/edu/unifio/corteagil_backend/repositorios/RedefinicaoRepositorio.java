package br.edu.unifio.corteagil_backend.repositorios;

import br.edu.unifio.corteagil_backend.entidades.RedefinicaoSenha;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RedefinicaoRepositorio extends JpaRepository<RedefinicaoSenha, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RedefinicaoSenha r where r.hash = :hash")
  Optional<RedefinicaoSenha> paraRedefinir(@Param("hash") String hash);
}
