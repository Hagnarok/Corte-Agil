package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record CadastroRequest(
    @NotBlank(message = "Informe o nome.")
        @Size(max = 80, message = "O nome deve ter até 80 caracteres.")
        String nome,
    @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail deve ter até 150 caracteres.")
        String email,
    @Size(max = 72, message = "A senha deve ter até 72 caracteres.") String senha,
    @Pattern(regexp = "ADMIN|ATENDENTE", message = "O perfil deve ser ADMIN ou ATENDENTE.")
        String perfil,
    boolean ativo,
    @Size(max = 36, message = "O identificador da categoria deve ter até 36 caracteres.")
        String categoriaId) {}
