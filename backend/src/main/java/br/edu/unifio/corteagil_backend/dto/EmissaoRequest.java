package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record EmissaoRequest(
    @NotBlank String service, @Pattern(regexp = "app|totem") @NotNull String origin) {}
