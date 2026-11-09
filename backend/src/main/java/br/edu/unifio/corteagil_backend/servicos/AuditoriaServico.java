package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.repositorios.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditoriaServico {
  private final AuditoriaRepositorio repo;

  public void registrar(String acao, String alvo, Usuario responsavel) {
    var e =
        new EventoAuditoria(UUID.randomUUID().toString(), acao, alvo, System.currentTimeMillis());
    e.setResponsavel(responsavel);
    e.setResponsavelNome(responsavel == null ? "Cliente / Totem" : responsavel.getUsername());
    repo.save(e);
  }
}
