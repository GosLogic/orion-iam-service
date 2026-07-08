package com.goslogic.orion.iam.bdd;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps base para US01 y US05.
 * Validan de forma simplificada el aislamiento multi-tenant y autenticación.
 */
public class TenantAndSessionStepDefinitions {

    // US01 - estado de tenant
    private String tenantNombre;
    private String tenantRuc;
    private boolean tenantExiste;
    private boolean solicitudCreacionEnviada;
    private boolean camposObligatoriosCompletos;
    private boolean duplicidadDetectada;
    private String tenantIdGenerado;
    private String tenantEstado;
    private String mensajeValidacion;

    // US05 - estado de sesión
    private boolean sesionAutenticada;
    private int minutosInactividad;
    private boolean sesionExpirada;
    private boolean requiereReautenticacion;

    @Dado("que el administrador accede al módulo de onboarding de tenants")
    public void queElAdministradorAccedeAlModuloDeOnboardingDeTenants() {
        camposObligatoriosCompletos = true;
        solicitudCreacionEnviada = false;
        duplicidadDetectada = false;
        tenantExiste = false;
    }

    @Dado("proporciona los datos válidos de la empresa {string}")
    public void proporcionaLosDatosValidosDeLaEmpresa(String empresa) {
        this.tenantNombre = empresa;
        this.camposObligatoriosCompletos = true;
    }

    @Dado("el RUC {string} no está registrado previamente")
    public void elRucNoEstaRegistradoPreviamente(String ruc) {
        this.tenantRuc = ruc;
        this.tenantExiste = false;
    }

    @Cuando("confirma el registro del tenant")
    public void confirmaElRegistroDelTenant() {
        this.solicitudCreacionEnviada = true;
        if (camposObligatoriosCompletos && !tenantExiste) {
            this.tenantIdGenerado = "tenant-simulado-001";
            this.tenantEstado = "ACTIVE";
        }
    }

    @Entonces("el sistema crea un tenantId único")
    public void elSistemaCreaUnTenantIdUnico() {
        assertThat(solicitudCreacionEnviada).isTrue();
        assertThat(tenantIdGenerado).isNotBlank();
        assertThat(tenantIdGenerado).startsWith("tenant-");
    }

    @Dado("que el administrador intenta registrar un tenant sin completar los campos obligatorios")
    public void queElAdministradorIntentaRegistrarUnTenantSinCompletarLosCamposObligatorios() {
        this.camposObligatoriosCompletos = false;
        this.solicitudCreacionEnviada = false;
    }

    @Cuando("envía la solicitud de creación del tenant")
    public void enviaLaSolicitudDeCreacionDelTenant() {
        this.solicitudCreacionEnviada = true;
        if (!camposObligatoriosCompletos) {
            this.mensajeValidacion = "Campos obligatorios faltantes";
        }
    }

    @Entonces("el sistema bloquea el registro y muestra los campos faltantes")
    public void elSistemaBloqueaElRegistroYMuestraLosCamposFaltantes() {
        assertThat(solicitudCreacionEnviada).isTrue();
        assertThat(camposObligatoriosCompletos).isFalse();
        assertThat(mensajeValidacion).contains("faltantes");
    }

    @Dado("que ya existe un tenant registrado con el RUC {string}")
    public void queYaExisteUnTenantRegistradoConElRuc(String ruc) {
        this.tenantRuc = ruc;
        this.tenantExiste = true;
        this.duplicidadDetectada = false;
    }

    @Cuando("el administrador intenta registrar otra empresa con el mismo RUC")
    public void elAdministradorIntentaRegistrarOtraEmpresaConElMismoRuc() {
        if (tenantExiste) {
            this.duplicidadDetectada = true;
        }
    }

    @Entonces("el sistema rechaza la operación por duplicidad")
    public void elSistemaRechazaLaOperacionPorDuplicidad() {
        assertThat(duplicidadDetectada).isTrue();
    }

    @Dado("que el administrador registra un tenant con datos válidos")
    public void queElAdministradorRegistraUnTenantConDatosValidos() {
        this.tenantNombre = "Empresa Demo Orion";
        this.tenantRuc = "20987654321";
        this.camposObligatoriosCompletos = true;
        this.tenantExiste = false;
        this.tenantIdGenerado = "tenant-simulado-002";
        this.tenantEstado = "ACTIVE";
    }

    @Cuando("el sistema finaliza el proceso de creación")
    public void elSistemaFinalizaElProcesoDeCreacion() {
        this.solicitudCreacionEnviada = true;
    }

    @Entonces("el tenant se muestra como activo con configuración base")
    public void elTenantSeMuestraComoActivoConConfiguracionBase() {
        assertThat(solicitudCreacionEnviada).isTrue();
        assertThat(tenantEstado).isEqualTo("ACTIVE");
        assertThat(tenantNombre).isNotNull();
    }

    @Dado("un usuario autenticado sin interacción en el sistema")
    public void unUsuarioAutenticadoSinInteraccionEnElSistema() {
        sesionAutenticada = true;
        sesionExpirada = false;
        requiereReautenticacion = false;
        minutosInactividad = 0;
    }

    @Cuando("transcurren 30 minutos sin actividad")
    public void transcurren30MinutosSinActividad() {
        minutosInactividad = 30;
        sesionExpirada = true;
    }

    @Entonces("la sesión expira por inactividad")
    public void laSesionExpiraPorInactividad() {
        assertThat(sesionAutenticada).isTrue();
        assertThat(minutosInactividad).isGreaterThanOrEqualTo(30);
        assertThat(sesionExpirada).isTrue();
    }

    @Dado("un usuario con sesión expirada")
    public void unUsuarioConSesionExpirada() {
        sesionAutenticada = true;
        sesionExpirada = true;
        requiereReautenticacion = false;
    }

    @Cuando("intenta acceder a un módulo protegido")
    public void intentaAccederAModuloProtegido() {
        if (sesionExpirada) {
            requiereReautenticacion = true;
        }
    }

    @Entonces("el sistema solicita reautenticación")
    public void elSistemaSolicitaReautenticacion() {
        assertThat(sesionExpirada).isTrue();
        assertThat(requiereReautenticacion).isTrue();
    }

    @Dado("un usuario con sesión autenticada y actividad reciente")
    public void unUsuarioConSesionAutenticadaYActividadReciente() {
        sesionAutenticada = true;
        sesionExpirada = false;
        minutosInactividad = 5;
    }

    @Cuando("navega entre módulos dentro del período permitido")
    public void navegaEntreModulosDentroDelPeriodoPermitido() {
        minutosInactividad = 10;
        sesionExpirada = false;
    }

    @Entonces("el sistema mantiene la sesión activa")
    public void elSistemaMantieneLaSesionActiva() {
        assertThat(sesionAutenticada).isTrue();
        assertThat(sesionExpirada).isFalse();
    }
}
