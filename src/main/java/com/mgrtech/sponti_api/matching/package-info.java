/**
 * Matching application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"availability::api", "contact::api", "user::api", "shared::api", "shared::error"}
)
package com.mgrtech.sponti_api.matching;
