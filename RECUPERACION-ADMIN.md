# Recuperación administrativa local

Con Java 21, compila el proyecto y configura las variables PostgreSQL para la base que contiene la cuenta que quieres recuperar. Usa una consola interactiva. El rol de conexión debe ser el propietario de esa base o un superusuario, sin cambio de rol.

```powershell
.\mvnw.cmd package -DskipTests
java -jar target/sistemaweb-0.0.1-SNAPSHOT.jar --recuperar-admin
```

El comando funciona sin servidor web. Selecciona una cuenta administradora activa e introduce la nueva contraseña en la consola de entrada oculta. La operación rechaza tablas de usuarios con triggers activos, invalida sesiones y enlaces anteriores cuando el esquema lo permite y verifica la autenticación antes de confirmar. No guardes contraseñas en argumentos ni en Git.

Para apuntar a la base de pruebas, activa el perfil `pruebas` y configura sus credenciales antes de ejecutar el comando. Comprueba el nombre de la base antes de recuperar una cuenta.
