package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record CadastroRequest(
    @NotBlank @Size(max = 80) String nome,
    @Email @Size(max = 150) String email,
    @Size(max = 72) String senha,
    @Pattern(regexp = "ADMIN|ATENDENTE") String perfil,
    boolean ativo,
    @Size(max = 36) String categoriaId) {}
