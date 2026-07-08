# language: es
@iam @multi-tenant @us01
Característica: US01 - Configuración de Tenant
  Como administrador de la plataforma Orion
  Quiero registrar y gestionar tenants de forma aislada
  Para garantizar el aislamiento multi-tenant y autenticación segura por empresa

  @tenant @happy-path
  Escenario: Crear tenant con datos válidos
    Dado que el administrador accede al módulo de onboarding de tenants
    Y proporciona los datos válidos de la empresa "Transportes Lima SAC"
    Y el RUC "20987654321" no está registrado previamente
    Cuando confirma el registro del tenant
    Entonces el sistema crea un tenantId único

  @tenant @validacion
  Escenario: Validar campos obligatorios
    Dado que el administrador intenta registrar un tenant sin completar los campos obligatorios
    Cuando envía la solicitud de creación del tenant
    Entonces el sistema bloquea el registro y muestra los campos faltantes

  @tenant @duplicidad
  Escenario: Evitar duplicidad de empresa
    Dado que ya existe un tenant registrado con el RUC "20123456789"
    Cuando el administrador intenta registrar otra empresa con el mismo RUC
    Entonces el sistema rechaza la operación por duplicidad

  @tenant @estado-inicial
  Escenario: Confirmar estado inicial del tenant
    Dado que el administrador registra un tenant con datos válidos
    Cuando el sistema finaliza el proceso de creación
    Entonces el tenant se muestra como activo con configuración base
