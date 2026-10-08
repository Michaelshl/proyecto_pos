package com.pos.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsignarRolRequest {
    @NotBlank
    private String rol;
}
