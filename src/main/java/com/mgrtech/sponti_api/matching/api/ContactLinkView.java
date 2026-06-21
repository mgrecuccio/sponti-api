package com.mgrtech.sponti_api.matching.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Contact Link response.")
public record ContactLinkView(
        @Schema(description = "Link Type.", example = "WHATSAPP")
        ContactLinkType type,
        @Schema(description = "WhatsApp url.", example = "https://wa.me/32470123456")
        String url,
        @Schema(description = "WhatsApp url.", example = "2026-06-12T12:01:00Z")
        Instant expiresAt
) {
}
