package com.pos.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|TI|NIT|PP", message = "El tipo de documento debe ser CC, CE, TI, NIT o PP")
    private String tipoDocumento;

    // Letras, dígitos y guion (un NIT lleva el dígito de verificación: 900123456-7).
    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9-]{4,20}",
            message = "El número de documento debe tener entre 4 y 20 caracteres (letras, números o guion)")
    private String numeroDocumento;

    // Para un NIT es la razón social de la empresa.
    @NotBlank(message = "El nombre (o la razón social, si es NIT) es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    // Obligatorio para las personas y no aplica a un NIT: esa regla depende del tipo de documento
    // y la valida el servicio (aquí solo se limita el largo).
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
