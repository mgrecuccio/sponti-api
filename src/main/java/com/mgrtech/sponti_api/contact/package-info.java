/**
 * Contact application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user::api", "shared::error", "shared::utils", "shared::validation"}
)
package com.mgrtech.sponti_api.contact;
