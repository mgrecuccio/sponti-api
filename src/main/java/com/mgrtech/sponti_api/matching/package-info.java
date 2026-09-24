/**
 * Matching application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"availability::api", "contact::api", "user::api", "shared::api", "shared::error", "shared::observability", "shared::utils"}
)
package com.mgrtech.sponti_api.matching;
