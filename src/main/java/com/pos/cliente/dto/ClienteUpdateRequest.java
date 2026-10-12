package com.pos.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// El número de documento es la llave primaria y no se edita (RF-021): viaja en la ruta.
@Data
public class ClienteUpdateRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|TI|NIT|PP", message = "El tipo de documento debe ser CC, CE, TI, NIT o PP")
    private String tipoDocumento;

    // Para un NIT es la razón social de la empresa.
    @NotBlank(message = "El nombre (o la razón social, si es NIT) es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    // Obligatorio para las personas y no aplica a un NIT (lo valida el servicio según el tipo de documento).
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String apellido;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 150, message = "El correo electrónico no puede superar 150 caracteres")
    private String email;

    @Pattern(regexp = "|[0-9+()\\- ]{7,20}",
            message = "El teléfono debe tener entre 7 y 20 caracteres (números, espacios, +, - y paréntesis)")
    private String telefono;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
}
