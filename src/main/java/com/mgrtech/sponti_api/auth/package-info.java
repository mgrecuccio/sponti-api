/**
 * Authentication application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user::api", "shared::error", "shared::utils", "shared::observability"}
)
package com.mgrtech.sponti_api.auth;
