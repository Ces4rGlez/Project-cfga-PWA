package com.proyecto.servicios.entity.gestopago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "productos_gestopago")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEntity {

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "producto")
    private String producto;

    @Column(name = "servicio")
    private String servicio;

    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    @Column(name = "precio")
    private String precio;

    @Column(name = "show_ayuda")
    private Boolean showAyuda;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;
}
