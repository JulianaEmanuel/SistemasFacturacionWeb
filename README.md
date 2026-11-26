# SistemaFacturacionWeb

Sistema de facturación con Java 21, Spring Boot 4, Spring Data JPA,
Spring Security, Thymeleaf, Bootstrap y PostgreSQL.


## PostgreSQL y ambiente de pruebas

El arranque normal usa PostgreSQL en el puerto web 8080. Configura
DB_HOST, DB_PORT, DB_NAME, DB_USER y DB_PASSWORD en el entorno del proceso.
El nombre predeterminado de la base normal es sistemafacturacionweb. Si tu base se creó como `SistemaFacturacionWeb`, configura ese nombre exacto en `DB_NAME`.

1. Crea una base PostgreSQL llamada sistemafacturacionweb_pruebas y un
   usuario independiente, facturacion_pruebas, con acceso a esa base.
   Ejecuta db/esquema_postgresql.sql y luego db/datos_prueba_postgresql.sql
   conectada a esa base. Puedes usar el editor SQL de pgAdmin o
   psql con -v ON_ERROR_STOP=1 -f para cada archivo.
2. En PowerShell configura DB_PRUEBAS_PASSWORD. Si corresponde, configura
   DB_PRUEBAS_HOST, DB_PRUEBAS_PORT y DB_PRUEBAS_USER. El puerto de la
   aplicación de pruebas es 8081 y su sesión usa una cookie independiente.
3. Compila con .\mvnw.cmd package -DskipTests. Para crear la cuenta inicial,
   ejecuta el primer comando y sigue sus preguntas interactivas. Luego
   ejecuta el segundo comando para abrir la instancia de pruebas.

   ### Comandos de PowerShell

       $env:SPRING_PROFILES_ACTIVE = 'pruebas'
       java -jar target/sistemaweb-0.0.1-SNAPSHOT.jar --bootstrap-admin
       java -jar target/sistemaweb-0.0.1-SNAPSHOT.jar

   La cuenta inicial se crea en la base de pruebas. Desde Empleados, asocia
   un empleado ficticio a esa cuenta si se requiere para registrar facturas.
4. Arranca la instancia normal en otra terminal con SPRING_PROFILES_ACTIVE
   configurado como postgres. En su menú, el enlace **Entrar a pruebas**
   abre http://127.0.0.1:8081/login por defecto. APP_URL_PRUEBAS permite
   ajustar el destino por despliegue.

Para entrar a pruebas desde una sesión administrativa, establece
APP_PASE_PRUEBAS_CLAVE con el mismo valor Base64 de 32 bytes aleatorios
en ambas instancias. En la principal, configura APP_PASE_PRUEBAS_ORIGEN
con la cuenta administradora autorizada para usar el botón. En pruebas,
APP_PASE_PRUEBAS_DESTINO identifica la cuenta administradora de destino
y usa admin_pruebas por defecto. Protege la clave como una credencial
de despliegue y usa HTTPS si las instancias se exponen fuera del equipo.
El pase vence en 30 segundos, se consume una sola vez y abre una sesión
separada en pruebas.

El envío de XML se habilita en la instancia de pruebas y apunta al servicio
de certificación del SRI con código 1. Los datos ficticios sirven para
ensayar el registro y generar XML localmente. La autorización remota
requiere un emisor habilitado y su firma electrónica. El correo de prueba
se dirige al SMTP local del puerto 1025.

## Funcionalidades

- Gestión de clientes, proveedores, productos y empleados.
- Administración de usuarios y tipos de usuario con permisos por rol.
- Inicio y cierre de sesión; contraseñas Argon2id y protección CSRF.
- Facturas con múltiples productos, cantidades decimales e IVA histórico.
- Reportes de ventas, productos, empleados y clientes; PDF, Excel e impresión.
- Comandos locales protegidos para creación inicial y recuperación administrativa.

## Requisitos

- Java 21, Maven o el wrapper incluido y PostgreSQL.
- Inicializa solamente una base nueva con [el esquema](db/esquema_postgresql.sql). Hibernate conserva `ddl-auto=none`.
- Usa [la guía de arranque](ARRANQUE-LOCAL.md) para conectar ambas instancias y trabajar con respaldos.

## Documentación

- [Rediseño completo y listado de todas las vistas](REDISENO-COMPLETO.md)

- [Seguridad y primera cuenta administradora](SEGURIDAD-USUARIOS.md)
- [Recuperación local del administrador](RECUPERACION-ADMIN.md)
- [Facturación](FACTURAS.md)
- [Reportes y exportaciones](REPORTES.md)

Los JAR, logs, archivos de entorno, respaldos SQL y configuraciones locales del
IDE se excluyen del repositorio. Las rutas a JAR separados mencionadas en las
guías corresponden a empaquetados locales y no se incluyen en Git.
