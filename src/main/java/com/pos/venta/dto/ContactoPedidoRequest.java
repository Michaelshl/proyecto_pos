package com.pos.venta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Contacto de una nota de pedido sin cliente registrado. Solo el celular es obligatorio aquí: si ya está
// registrado se usa el contacto guardado; si es nuevo, el servicio exige nombre, correo y dirección.
@Data
public class ContactoPedidoRequest {

    @Pattern(regexp = "[0-9+()\\- ]{7,20}",
            message = "El celular debe tener entre 7 y 20 caracteres (números, espacios, +, - y paréntesis)")
    private String telefono;

    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 150, message = "El correo electrónico no puede superar 150 caracteres")
    private String correo;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
}
