# language: es
@iam @jwt @us04
Característica: US04 - Autenticación JWT
  Como usuario de la plataforma Orion
  Quiero autenticarme con credenciales y token JWT
  Para proteger el aislamiento multi-tenant y autenticación por sesión

  @login-exitoso
  Escenario: Login exitoso
    Dado un usuario activo con credenciales válidas
    Cuando solicita autenticarse en el IAM
    Entonces el sistema emite un token JWT válido

  @login-fallido
  Escenario: Login fallido
    Dado un usuario que ingresa credenciales incorrectas
    Cuando solicita autenticarse en el IAM
    Entonces el sistema muestra mensaje de error de autenticación

  @token-invalido
  Escenario: Acceso con token inválido
    Dado un token JWT inválido para acceder a un recurso protegido
    Cuando el sistema valida el token
    Entonces responde con estado no autorizado
