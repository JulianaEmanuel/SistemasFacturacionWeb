# Usuarios, tipos y acceso

Todos los módulos requieren sesión. `/modulos/usuarios` y `/modulos/tipos-usuario`
requieren el tipo administrativo reservado, tanto en HTTP como en el servicio.
`ADMIN_TYPE` configura su nombre (por defecto `ADMINISTRADOR`); no cambies esta
variable después de crear el catálogo sin un procedimiento de migración.
Los IDs se generan en la base y no determinan privilegios.

## Crear la primera cuenta (operación manual)

Con una base nueva, configura Java 21 y la conexión PostgreSQL siguiendo la [guía local](ARRANQUE-LOCAL.md). Compila con `./mvnw.cmd package -DskipTests` y ejecuta desde una consola interactiva:

```powershell
java -jar target/sistemaweb-0.0.1-SNAPSHOT.jar --bootstrap-admin
```

El comando ejecuta la creación en una sola transacción y exige una base sin usuarios. Solicita el nombre y la contraseña con entrada oculta. Para una cuenta existente usa [la recuperación administrativa local](RECUPERACION-ADMIN.md).

## Iniciar la aplicación

Ejecuta `./mvnw.cmd spring-boot:run` con las variables del ambiente correspondiente.

Abre `http://localhost:8080/login` e introduce la cuenta creada manualmente.
El cierre de sesión usa POST con CSRF. Los formularios Thymeleaf incluyen el token
automáticamente mediante `th:action`; no se ha desactivado CSRF.

La aplicación escucha por defecto solo en `127.0.0.1`. Para acceso remoto configura
`SERVER_ADDRESS`, HTTPS y `SESSION_COOKIE_SECURE=true` antes de usar
credenciales reales. El valor false permite únicamente el desarrollo por HTTP local.
Las sesiones expiran a los 20 minutos de inactividad y la cookie es HttpOnly/SameSite=Lax.
El identificador de sesión cambia al autenticar.

## Credenciales y privilegios

Argon2id utiliza Spring Security Crypto y Bouncy Castle 1.86: 19 MiB de memoria,
2 iteraciones, paralelismo 1, sal aleatoria de 16 bytes y salida de 32 bytes.
La representación codificada completa se almacena como UTF-8 en `BYTEA`.
No existe compatibilidad con el SHA-256 del sistema original. Los parámetros deben
revisarse con mediciones en el servidor destino antes de un despliegue de producción.

Solo `AutenticacionUsuarios` selecciona el hash para autenticar. Los modelos enviados
a las vistas y el modelo JPA `Usuario` usado por Empleados no incluyen credenciales.
La edición ordinaria actualiza únicamente nombre y tipo; contraseña y estado tienen
acciones independientes. No existe eliminación física de usuarios.

Los permisos se verifican con los valores actuales de la base, no con roles antiguos
guardados en sesión. La identidad numérica de la cuenta se vincula a la sesión para
que un nombre reutilizado no herede una sesión anterior. Una cuenta desactivada o
renombrada debe iniciar sesión otra vez. Los cambios de contraseña incrementan la
versión de credencial y revocan los enlaces de recuperación pendientes; el filtro
invalida las sesiones anteriores en su siguiente solicitud. Una cuenta desactivada
no puede utilizar sus sesiones mientras permanezca inactiva.

Todas las modificaciones administrativas y la inicialización adquieren el mismo bloqueo transaccional y
revisan nuevamente los permisos y el número de administradores activos. El tipo
reservado no se puede renombrar ni eliminar. No se puede desactivar ni cambiar a
otro tipo al último administrador activo. Las escrituras directas externas a la
aplicación deben respetar estas invariantes: no hay nuevas restricciones SQL.

La adquisición exige una transacción Spring existente. Con el driver PostgreSQL,
de tabla. El repositorio inicia entonces la transacción física mediante una lectura
de catálogo sin escrituras y verifica su estado antes de solicitar el bloqueo.
La llamada usa `CallableStatement` y lee explícitamente el valor RETURN:
0/1 éxito, -1 timeout, -2 cancelación, -3 interbloqueo, -999 error de llamada.
Un -999 no se presenta como concurrencia. No se emiten commits parciales ni se
libera el bloqueo antes de terminar la transacción de creación o modificación.

Los nombres duplicados se comprueban mediante igualdad en PostgreSQL y, por ello,
con la intercalación real de las columnas. Un tipo asociado a usuarios no se elimina.
La desactivación conserva empleados, personas y facturas. Empleados sigue ofreciendo
solo usuarios activos no asignados para nuevas asociaciones y permite conservar el
usuario actual inactivo.

## Recuperación de contraseña

El esquema PostgreSQL incluye las tablas de seguridad. Después de incorporar el versionado en una instalación existente, reinicia la aplicación para que las sesiones antiguas se autentiquen otra vez.

Cada usuario debe registrar y verificar su propio correo desde **Mi perfil → Seguridad
de la cuenta**. No se importan correos de Persona ni se considera verificada una dirección
por existir en otro registro. El correo anterior permanece asociado hasta confirmar el
nuevo. La recuperación pública solo envía al correo verificado de una cuenta activa y
siempre muestra la misma respuesta. Los enlaces son aleatorios de 256 bits, se almacena
su SHA-256, expiran en 15 minutos y solo se consumen al enviar el formulario POST; abrir
el enlace no lo consume. Los intentos se limitan a 3 por destino/hora y 10 por IP/15
minutos. La tabla de límites guarda HMAC de destino e IP, no los valores originales.

Configura en el entorno del servidor, nunca en el repositorio:

- `RECOVERY_HMAC_KEY`: secreto aleatorio de al menos 32 bytes, distinto por ambiente.
- `RECOVERY_PUBLIC_BASE_URL`: origen HTTPS público completo que recibirá los enlaces.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`,
  `SMTP_STARTTLS` y `RECOVERY_MAIL_FROM`: configuración del proveedor de correo.

No uses los valores locales predeterminados en producción; configura HTTPS, SMTP
autenticado y STARTTLS según el proveedor. Sin el secreto HMAC, las solicitudes no
generan tokens. El comando local `--recuperar-admin` continúa separado y también
invalida las sesiones/versiones y enlaces pendientes cuando se cambia la contraseña.
CSRF permanece habilitado en los formularios públicos y autenticados.

## Rutas

- GET/POST `/login`; POST `/logout`; GET `/sin-permiso`.
- Público: GET/POST `/recuperar-contrasenia`, `/restablecer-contrasenia` y `/verificar-correo`.
- Con sesión: GET/POST `/cuenta/correo` para solicitar el enlace de verificación.
- GET `/modulos/tipos-usuario?q=...`; GET `/nuevo`; POST en la ruta base.
- GET/POST `/modulos/tipos-usuario/{id}/editar`; POST `/{id}/eliminar`.
- GET `/modulos/usuarios?q=...&tipo=...&activo=true|false`; GET `/nuevo`; POST en la ruta base.
- GET/POST `/modulos/usuarios/{id}/editar` y `/{id}/contrasenia`.
- POST `/modulos/usuarios/{id}/activar` y `/{id}/desactivar`.

## Pruebas

La suite prueba escrituras con mocks, incluida la inicialización. Las pruebas de
integración con PostgreSQL solo consultan datos no sensibles. Nunca ejecutes el
comando `--bootstrap-admin` como preparación automática de pruebas.

```powershell
.\mvnw.cmd test
```

Los cambios de las pruebas existentes agregan sesiones simuladas y CSRF; no deshabilitan
los filtros de seguridad. Se conserva el comportamiento funcional de los módulos.

## Archivos de esta implementación

Nuevos, bajo `src/main/java/com/facturacion/sistemaweb/`:

- `model/TipoUsuario.java`, `TipoUsuarioForm.java`, `UsuarioForm.java`, `UsuarioResumen.java`.
- `repository/TipoUsuarioRepository.java`, `AdministracionRepository.java`.
- `service/AdministracionService.java`, `AdministracionException.java`.
- `security/SecurityConfig.java`, `AdminAccess.java`, `AutenticacionUsuarios.java`,
  `SesionVigenteFilter.java`, `ClaveSegura.java`, `BootstrapAdminService.java`, `BootstrapAdminRunner.java`.
- `controller/AdministracionController.java`, `AccesoController.java`, `CuentaActualAdvice.java`.

Nuevas vistas bajo `src/main/resources/templates/`:

- `login.html`, `sin-permiso.html`.
- `tipos-usuario/lista.html`, `tipos-usuario/formulario.html`.
- `usuarios/lista.html`, `usuarios/formulario.html`, `usuarios/contrasenia.html`.

Pruebas nuevas bajo `src/test/java/com/facturacion/sistemaweb/`:

- `AdministracionServiceTest.java`, `AdministracionSecurityTest.java`, `AdministracionMethodSecurityTest.java`.
- `AdministracionConcurrencyTest.java`, `AdministracionRepositoryTest.java`, `AdministracionReadOnlyIntegrationTest.java`.
- `ClaveSeguraTest.java`, `BootstrapAdminTest.java`.

Archivos existentes modificados:

- `pom.xml`: dependencias de seguridad, criptografía y pruebas.
- `SistemawebApplication.java`: entrada explícita al comando sin servidor web.
- `src/main/resources/application.properties`: configuración de acceso local, sesión y diagnóstico.
- `templates/fragments/sidebar.html`, `topbar.html`: enlace de Tipos, cuenta actual y cierre de sesión.
- `ClienteControllerIntegrationTest.java`, `ClienteFechaViewTest.java`, `EmpleadoControllerTest.java`,
  `EmpleadoReadOnlyIntegrationTest.java`, `ProductoControllerIntegrationTest.java`,
  `ProveedorControllerIntegrationTest.java`: sesión simulada y tokens CSRF.

`Usuario.java`, `UsuarioRepository.java`, los servicios/controladores de los módulos
