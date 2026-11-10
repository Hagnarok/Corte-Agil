package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.dto.SenhaResposta;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaServico {
  private final CategoriaServicoRepositorio categorias;
  private final ServicoRepositorio servicos;
  private final SenhaRepositorio senhas;

  @Transactional(readOnly = true)
  public Object servicosDaCategoria(String id) {
    if (!categorias.existsById(id))
      throw new RegraNegocioException("Categoria não encontrada.", 404);
    return servicos.findByCategoriaIdOrderByNomeAsc(id);
  }

  @Transactional(readOnly = true)
  public Object senhasPorSituacao(String status) {
    if (!java.util.List.of("waiting", "called", "serving", "done", "absent").contains(status))
      throw new RegraNegocioException("Situação inválida.", 400);
    return senhas.findByStatusOrderByCreatedAtAsc(status).stream().map(SenhaResposta::of).toList();
  }
}
