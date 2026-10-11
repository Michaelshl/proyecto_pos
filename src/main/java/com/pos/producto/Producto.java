package com.pos.producto;

import com.pos.categoria.Categoria;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "productos")
public class Producto {

    // El código es la llave primaria: dato propio del producto, único e inmutable.
    @Id
    @Column(length = 30)
    private String codigo;

    @Column(length = 150, nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "precio_venta", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioVenta;

    private Integer stock;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(columnDefinition = "TINYINT(1)")
    private Boolean activo;

    // Se guarda al inactivar el producto (RF-017).
    @Column(name = "motivo_inactivacion", length = 500)
    private String motivoInactivacion;

    // Bloqueo optimista: si dos ajustes o ventas tocan el mismo producto a la vez,
    // el segundo falla con 409 en vez de pisar el stock del primero.
    @Version
    private Long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_nombre")
    private Categoria categoria;
}
