# Datos ficticios para PostgreSQL

Aplica primero [el esquema PostgreSQL](db/esquema_postgresql.sql) en una base nueva denominada `sistemafacturacionweb_pruebas`. Después ejecuta [el lote de datos](db/datos_prueba_postgresql.sql) con pgAdmin o con `psql -v ON_ERROR_STOP=1 -d sistemafacturacionweb_pruebas -f db/datos_prueba_postgresql.sql`.

El lote se limita a pruebas y agrega personas, un proveedor y productos ficticios. La transacción evita inserciones parciales y repetir el lote conserva los registros existentes. Para crear facturas desde la interfaz se necesita una cuenta asociada a un empleado.
