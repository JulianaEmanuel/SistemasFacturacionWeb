# Verificación de correo y recuperación de contraseña

Cada usuario autenticado puede verificar su correo desde **Perfil → Seguridad de la cuenta**. El enlace vence en 15 minutos y se consume una vez. El cambio de contraseña con sesión iniciada está disponible en **Perfil → Cambiar contraseña**.

Para recibir mensajes reales, configura `CORREO_BANDEJA_LOCAL=false`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` y, según el proveedor, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_STARTTLS_REQUIRED` o `SMTP_SSL`. Usa como `SMTP_FROM` una dirección autorizada por el remitente. Inyecta las credenciales al proceso y mantenlas fuera de Git.

Configura también `RECOVERY_HMAC_KEY` con un secreto estable de al menos 32 bytes y `RECOVERY_PUBLIC_BASE_URL` con el origen que el destinatario debe abrir. Si usas dos instancias locales, los enlaces de 8080 y 8081 corresponden a bases y sesiones distintas.

En pruebas se habilita la bandeja local de forma predeterminada para direcciones ficticias terminadas en `@example.invalid`. Usa un valor explícito de `CORREO_BANDEJA_LOCAL` para cambiar ese comportamiento.
