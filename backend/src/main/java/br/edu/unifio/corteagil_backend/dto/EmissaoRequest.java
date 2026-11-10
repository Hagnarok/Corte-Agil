package br.edu.unifio.corteagil_backend.dto;

import jakarta.validation.constraints.*;

public record EmissaoRequest(
    @NotBlank(message = "Informe o serviço.") String service,
    @Pattern(regexp = "app|totem", message = "A origem deve ser app ou totem.")
        @NotNull(message = "Informe a origem.")
        String origin) {}
