/**
 * User application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"sms::api", "shared::error", "shared::utils", "shared::validation"}
)
package com.mgrtech.sponti_api.user;
