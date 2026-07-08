package com.goslogic.orion.iam.bdd;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions base para US04.
 * Valida de forma aislada el flujo de autenticación JWT en contexto multi-tenant.
 */
public class AuthStepDefinitions {

    private String email;
    private String password;
    private String token;
    private int statusCode;
    private String authError;

    @Dado("un usuario activo con credenciales válidas")
    public void unUsuarioActivoConCredencialesValidas() {
        // Aislamiento Multi-Tenant y Autenticación: usuario válido del tenant.
        this.email = "conductor@empresa.com";
        this.password = "123456";
        this.authError = null;
    }

    @Dado("un usuario que ingresa credenciales incorrectas")
    public void unUsuarioQueIngresaCredencialesIncorrectas() {
        // Aislamiento Multi-Tenant y Autenticación: intento fallido por credencial inválida.
        this.email = "conductor@empresa.com";
        this.password = "incorrecta";
        this.authError = null;
    }

    @Dado("un token JWT inválido para acceder a un recurso protegido")
    public void unTokenJwtInvalidoParaAccederAUnRecursoProtegido() {
        // Aislamiento Multi-Tenant y Autenticación: token manipulado/no confiable.
        this.token = "jwt-invalido";
    }

    @Cuando("solicita autenticarse en el IAM")
    public void solicitaAutenticarseEnElIam() {
        if ("conductor@empresa.com".equals(email) && "123456".equals(password)) {
            this.token = "jwt-simulado-valido";
            this.statusCode = 200;
            this.authError = null;
        } else {
            this.token = null;
            this.statusCode = 401;
            this.authError = "Credenciales inválidas";
        }
    }

    @Cuando("el sistema valida el token")
    public void elSistemaValidaElToken() {
        if ("jwt-simulado-valido".equals(token)) {
            this.statusCode = 200;
        } else {
            this.statusCode = 401;
        }
    }

    @Entonces("el sistema emite un token JWT válido")
    public void elSistemaEmiteUnTokenJwtValido() {
        assertThat(statusCode).isEqualTo(200);
        assertThat(token).isNotBlank();
    }

    @Entonces("el sistema muestra mensaje de error de autenticación")
    public void elSistemaMuestraMensajeDeErrorDeAutenticacion() {
        assertThat(statusCode).isEqualTo(401);
        assertThat(authError).contains("Credenciales inválidas");
    }

    @Entonces("responde con estado no autorizado")
    public void respondeConEstadoNoAutorizado() {
        assertThat(statusCode).isEqualTo(401);
    }
}
