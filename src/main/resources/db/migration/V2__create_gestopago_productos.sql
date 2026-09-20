CREATE TABLE productos_gestopago (
    id_producto INT PRIMARY KEY,
    producto VARCHAR(255),
    servicio VARCHAR(255),
    id_servicio INT,
    id_cat_tipo_servicio INT,
    tipo_front INT,
    has_digito_verificador BOOLEAN,
    tipo_referencia VARCHAR(255),
    precio VARCHAR(255),
    show_ayuda BOOLEAN,
    legend VARCHAR(1000)
);
