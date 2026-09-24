/**
 * Availability application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user::api", "shared::api", "shared::error", "shared::utils"}
)
package com.mgrtech.sponti_api.availability;
