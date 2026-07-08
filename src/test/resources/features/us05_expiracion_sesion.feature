# language: es
@iam @sesion @us05
Característica: US05 - Expiración de Sesión
  Como usuario autenticado
  Quiero que la sesión expire o continúe según mi actividad
  Para asegurar el aislamiento multi-tenant y autenticación

  @expiracion
  Escenario: Expirar sesión por inactividad
    Dado un usuario autenticado sin interacción en el sistema
    Cuando transcurren 30 minutos sin actividad
    Entonces la sesión expira por inactividad

  @reautenticacion
  Escenario: Solicitar reautenticación
    Dado un usuario con sesión expirada
    Cuando intenta acceder a un módulo protegido
    Entonces el sistema solicita reautenticación

  @sesion-activa
  Escenario: Mantener sesión con actividad
    Dado un usuario con sesión autenticada y actividad reciente
    Cuando navega entre módulos dentro del período permitido
    Entonces el sistema mantiene la sesión activa
