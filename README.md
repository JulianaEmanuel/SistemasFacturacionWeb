# SistemaFacturacionWeb

Sistema de facturación con Java 21, Spring Boot 4, Spring Data JPA,
Spring Security, Thymeleaf, Bootstrap y PostgreSQL.


## Arranque

Con PostgreSQL configurado, abre PowerShell en la carpeta del proyecto y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

Abre [Facturación Web](http://127.0.0.1:8080/login).

El ambiente de pruebas usa el mismo comando en otra terminal, con el perfil
`pruebas`. Cuando esa instancia esté activa, puedes entrar desde el botón
**Entrar a pruebas** del menú principal. La preparación inicial y las variables
de cada ambiente están en la [guía de arranque local](ARRANQUE-LOCAL.md).

En pruebas, el envío de XML apunta a certificación del SRI (ambiente 1).
La autorización requiere un emisor habilitado y su firma electrónica.

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
