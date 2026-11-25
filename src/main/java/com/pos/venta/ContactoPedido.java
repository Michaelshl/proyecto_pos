package com.pos.venta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

// Contacto de una nota de pedido que NO es un cliente registrado: no va en `clientes` porque no es un cliente para
// facturación. La llave es su cédula. Se guarda para entregar el pedido y reconocerlo en los próximos.
@Data
@Entity
@Table(name = "contactos_pedido")
public class ContactoPedido {

    @Id
    @Column(length = 20)
    private String documento;

    @Column(length = 20, nullable = false)
    private String telefono;

    @Column(length = 150, nullable = false)
    private String nombre;

    @Column(length = 150, nullable = false)
    private String correo;

    @Column(length = 255, nullable = false)
    private String direccion;
}
