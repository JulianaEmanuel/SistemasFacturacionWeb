# Arranque local con PostgreSQL

Requiere Java 21 y PostgreSQL. Para una base nueva ejecuta `db/esquema_postgresql.sql` en la base correspondiente. Hibernate usa `ddl-auto=none`.

## Principal

Configura `DB_NAME` con el nombre exacto de tu base. La instalación local existente usa `SistemaFacturacionWeb`.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:DB_NAME = 'SistemaFacturacionWeb'
$env:DB_USER = 'postgres'
$clave = Read-Host 'Contraseña de PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $clave).Password
.\mvnw.cmd spring-boot:run
```

Abre http://127.0.0.1:8080/login.

## Pruebas

La base es `sistemafacturacionweb_pruebas`. Carga el esquema y `db/datos_prueba_postgresql.sql` en una base vacía. Usa un usuario de PostgreSQL dedicado con acceso a los objetos de pruebas. En otra terminal:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'pruebas'
$env:DB_PRUEBAS_USER = 'facturacion_pruebas'
$clave = Read-Host 'Contraseña de PostgreSQL de pruebas' -AsSecureString
$env:DB_PRUEBAS_PASSWORD = [System.Net.NetworkCredential]::new('', $clave).Password
.\mvnw.cmd spring-boot:run
```

Abre http://127.0.0.1:8081/login. El enlace con la sesión administrativa se configura según el [README](README.md).

Si la conexión es remota, configura el host, puerto y modo TLS mediante `DB_HOST`, `DB_PORT`, `DB_SSLMODE` o sus equivalentes `DB_PRUEBAS_HOST`, `DB_PRUEBAS_PORT`, `DB_PRUEBAS_SSLMODE`. Para verificar identidad del servidor usa `verify-full` con un certificado de confianza.

## Respaldos y restauración

Usa `pg_dump --format=custom --file <archivo.dump> --dbname <base>`. Conserva los respaldos fuera de Git porque contienen datos y hashes de contraseña. Inspecciona con `pg_restore --list <archivo.dump>`. Para ensayar una restauración, crea una base aparte y usa `pg_restore --exit-on-error --no-owner --no-privileges --dbname <base_nueva> <archivo.dump>`.

Las pruebas Maven de integración requieren una copia PostgreSQL dedicada. Usa el entorno de conexión de esa copia y evita ejecutar pruebas que escriben sobre una base operativa.
