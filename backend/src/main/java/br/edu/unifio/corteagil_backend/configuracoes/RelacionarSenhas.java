package br.edu.unifio.corteagil_backend.configuracoes;

import br.edu.unifio.corteagil_backend.repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
@RequiredArgsConstructor
public class RelacionarSenhas implements ApplicationRunner {
  private final SenhaRepositorio senhas;
  private final ServicoRepositorio servicos;
  private final BalcaoRepositorio balcoes;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (var s : senhas.findAll()) {
      if (s.getServico() == null) servicos.findByNome(s.getService()).ifPresent(s::setServico);
      if (s.getBalcao() == null) balcoes.findAll().stream().findFirst().ifPresent(s::setBalcao);
    }
  }
}
