package com.goslogic.orion.iam.infrastructure.client;

/**
 * Identidad resuelta en Fleet para un conductor IAM (login DRIVER).
 */
public record DriverFleetIdentity(String driverExternalId, String vehicleExternalId) {}
