package br.edu.unifio.corteagil_backend.servicos;

import br.edu.unifio.corteagil_backend.dto.*;
import br.edu.unifio.corteagil_backend.entidades.*;
import br.edu.unifio.corteagil_backend.excecoes.RegraNegocioException;
import br.edu.unifio.corteagil_backend.repositorios.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastroServico {
  private final UsuarioRepositorio usuarios;
  private final ServicoRepositorio servicos;
  private final BalcaoRepositorio balcoes;
  private final AuditoriaServico auditoria;

  public List<?> listar(String tipo) {
    return switch (tipo) {
      case "usuarios" -> usuarios.findAll();
      case "servicos" -> servicos.findAll();
      case "balcoes" -> balcoes.findAll();
      default -> throw new RegraNegocioException("Cadastro não encontrado.", 404);
    };
  }

  @Transactional
  public Object salvar(String tipo, String id, CadastroRequest d, Usuario ator) {
    String nome = d.nome().trim();
    Object result;
    switch (tipo) {
      case "usuarios" -> {
        var u =
            id == null
                ? new Usuario()
                : usuarios
                    .findById(id)
                    .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado.", 404));
        if (usuarios.findByUsername(nome).filter(x -> !x.getId().equals(id)).isPresent())
          throw new RegraNegocioException("Esse usuário já existe.", 409);
        if (d.email() != null
            && !d.email().isBlank()
            && usuarios
                .findByEmail(d.email().trim().toLowerCase())
                .filter(x -> !x.getId().equals(id))
                .isPresent()) throw new RegraNegocioException("E-mail já cadastrado.", 409);
        if (d.perfil() == null) throw new RegraNegocioException("Informe o perfil.", 400);
        if (u.getId() != null
            && u.getId().equals(ator.getId())
            && (!d.ativo() || !d.perfil().equals("ADMIN")))
          throw new RegraNegocioException(
              "Você não pode retirar seu próprio acesso de administrador.", 409);
        if (id == null) {
          u.setId(UUID.randomUUID().toString());
          if (d.senha() == null || d.senha().length() < 10)
            throw new RegraNegocioException("Informe uma senha com pelo menos 10 caracteres.", 400);
        }
        if (d.senha() != null && !d.senha().isBlank()) {
          if (d.senha().length() < 10
              || d.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new RegraNegocioException(
                "Senha deve ter pelo menos 10 caracteres e no máximo 72 bytes.", 400);
          u.setPasswordHash(new BCryptPasswordEncoder().encode(d.senha()));
          u.setVersaoSessao(u.getVersaoSessao() + 1);
        }
        if (u.isAtivo() != d.ativo() || !java.util.Objects.equals(u.getPerfil(), d.perfil()))
          u.setVersaoSessao(u.getVersaoSessao() + 1);
        u.setUsername(nome);
        u.setEmail(
            d.email() == null || d.email().isBlank() ? null : d.email().trim().toLowerCase());
        u.setPerfil(d.perfil());
        u.setAtivo(d.ativo());
        result = usuarios.save(u);
      }
      case "servicos" -> {
        var s =
            id == null
                ? new Servico()
                : servicos
                    .findById(id)
                    .orElseThrow(() -> new RegraNegocioException("Serviço não encontrado.", 404));
        if (id == null) s.setId(UUID.randomUUID().toString());
        s.setNome(nome);
        s.setAtivo(d.ativo());
        result = servicos.save(s);
      }
      case "balcoes" -> {
        var b =
            id == null
                ? new Balcao()
                : balcoes
                    .findById(id)
                    .orElseThrow(() -> new RegraNegocioException("Balcão não encontrado.", 404));
        if (id == null) b.setId(UUID.randomUUID().toString());
        b.setNome(nome);
        b.setAtivo(d.ativo());
        result = balcoes.save(b);
      }
      default -> throw new RegraNegocioException("Cadastro inválido.", 404);
    }
    auditoria.registrar(id == null ? "Cadastro criado" : "Cadastro atualizado", nome, ator);
    return result;
  }

  @Transactional
  public void excluir(String tipo, String id, Usuario ator) {
    if (tipo.equals("usuarios")) {
      var u =
          usuarios
              .findById(id)
              .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado.", 404));
      if (id.equals(ator.getId()))
        throw new RegraNegocioException("Você não pode excluir sua própria conta.", 409);
      usuarios.delete(u);
    } else if (tipo.equals("servicos"))
      servicos.delete(
          servicos
              .findById(id)
              .orElseThrow(() -> new RegraNegocioException("Serviço não encontrado.", 404)));
    else if (tipo.equals("balcoes"))
      balcoes.delete(
          balcoes
              .findById(id)
              .orElseThrow(() -> new RegraNegocioException("Balcão não encontrado.", 404)));
    else throw new RegraNegocioException("Cadastro inválido.", 404);
    auditoria.registrar("Cadastro excluído", tipo + " / " + id, ator);
  }
}
