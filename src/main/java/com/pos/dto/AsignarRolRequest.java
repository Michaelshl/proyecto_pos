package com.pos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarRolRequest {
    @NotNull
    private Long rolId;
}
