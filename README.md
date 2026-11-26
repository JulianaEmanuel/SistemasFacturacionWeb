# SistemaFacturacionWeb

Sistema de facturación con Java 21, Spring Boot 4, Spring Data JPA,
Spring Security, Thymeleaf, Bootstrap y SQL Server.


## PostgreSQL y ambiente de pruebas

El arranque normal usa PostgreSQL en el puerto web 8080. Configura
DB_HOST, DB_PORT, DB_NAME, DB_USER y DB_PASSWORD en el entorno del proceso.
El nombre predeterminado de la base normal es sistemafacturacionweb.

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

El envío de XML se habilita en la instancia de pruebas y apunta al servicio
de certificación del SRI con código 1. Los datos ficticios sirven para
ensayar el registro y generar XML localmente. La autorización remota
requiere un emisor habilitado y su firma electrónica. El correo de prueba
se dirige al SMTP local del puerto 1025.

Las instrucciones de SQL Server de las secciones siguientes corresponden
al arranque histórico con su perfil explícito.

## Funcionalidades

- Gestión de clientes, proveedores, productos y empleados.
- Administración de usuarios y tipos de usuario con permisos por rol.
- Inicio y cierre de sesión; contraseñas Argon2id y protección CSRF.
- Facturas con múltiples productos, cantidades decimales e IVA histórico.
- Reportes de ventas, productos, empleados y clientes; PDF, Excel e impresión.
- Comandos locales protegidos para creación inicial y recuperación administrativa.

## Requisitos

- Java 21 y Maven o el wrapper incluido.
- SQL Server y una base existente llamada `SistemaFacturacionWeb` con el esquema
  del proyecto. No se publican datos reales ni respaldos de la base.
- Para autenticación integrada en Windows, el DLL nativo correspondiente al
  controlador Microsoft JDBC, configurado mediante `java.library.path`.

Hibernate mantiene `spring.jpa.hibernate.ddl-auto=none`: la aplicación no crea
ni modifica automáticamente las tablas.

## Desarrollo local

Para el arranque local de SQL Server, sigue los perfiles y requisitos de
autenticación de [ARRANQUE-LOCAL.md](ARRANQUE-LOCAL.md). El proyecto usa `nodb`
como perfil Spring predeterminado; al ejecutar la aplicación con SQL Server,
activa `sqlserver` explícitamente para registrar los controladores de
recuperación y acceso a datos.

```powershell
.\mvnw.cmd spring-boot:run
```

El comando anterior usa el perfil predeterminado si no hay una configuración
Maven local que active `desarrollo-local`; en ese caso el servidor usa el
puerto 8080 y no incluye las funciones que dependen de SQL Server. La guía
indica el comando explícito para abrir la aplicación completa en el puerto
8080. Los comandos siguientes documentan la configuración manual alternativa.

Configurar las variables en la misma terminal PowerShell que inicia Java:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:DB_SERVER = '127.0.0.1'
$env:DB_PORT = '1433'
$env:DB_NAME = 'SistemaFacturacionWeb'
$env:DB_TRUST_SERVER_CERTIFICATE = 'true'
```

Adaptar las rutas de Java y del DLL al equipo. Las pruebas de integración usan
consultas de lectura a SQL Server; las escrituras de las pruebas son simuladas.

```powershell
.\mvnw.cmd package '-Dspring.profiles.active=sqlserver' '-Dspring.profiles.include=local' '-DargLine=-Djava.library.path=C:/mssql-jdbc-auth/13.4/x64'

& "$env:JAVA_HOME/bin/java.exe" '-Djava.library.path=C:/mssql-jdbc-auth/13.4/x64' -jar target/sistemaweb-0.0.1-SNAPSHOT.jar '--spring.profiles.active=sqlserver,local' '--server.port=8081'
```

Abrir `http://localhost:8081/login`. No existen credenciales predeterminadas ni
registro público de administradores. Consultar la guía de seguridad para crear
la primera cuenta mediante una terminal local con entrada oculta.

La excepción de certificado requiere el perfil `local` y un servidor loopback.
En producción conservar `encrypt=true`, la validación del certificado y una CA
de confianza; no activar el perfil local.

## Documentación

- [Rediseño completo y listado de todas las vistas](REDISENO-COMPLETO.md)

- [Seguridad y primera cuenta administradora](SEGURIDAD-USUARIOS.md)
- [Recuperación local del administrador](RECUPERACION-ADMIN.md)
- [SQL Server y certificados TLS](SQLSERVER-LOCAL.md)
- [Facturación](FACTURAS.md)
- [Reportes y exportaciones](REPORTES.md)

Los JAR, logs, archivos de entorno, respaldos SQL y configuraciones locales del
IDE se excluyen del repositorio. Las rutas a JAR separados mencionadas en las
guías corresponden a empaquetados locales y no se incluyen en Git.
