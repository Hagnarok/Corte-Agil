package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotNull(message = "Informe o usuário.")
        @Size(max = 100, message = "O usuário deve ter até 100 caracteres.")
        String username,
    @NotNull(message = "Informe a senha.")
        @Size(max = 200, message = "A senha de acesso excede o tamanho permitido.")
        String password) {}
