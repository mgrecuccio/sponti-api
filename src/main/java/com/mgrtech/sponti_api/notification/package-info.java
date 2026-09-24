/**
 * Notification application module.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"matching::api", "shared::api", "shared::error", "shared::observability", "shared::utils", "user::api"}
)
package com.mgrtech.sponti_api.notification;
