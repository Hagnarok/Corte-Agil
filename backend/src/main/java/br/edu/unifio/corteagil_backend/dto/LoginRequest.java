package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotNull @Size(max = 100) String username, @NotNull @Size(max = 200) String password) {}
