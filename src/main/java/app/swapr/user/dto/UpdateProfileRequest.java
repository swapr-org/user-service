package app.swapr.user.dto;

import java.math.BigDecimal;

/** The body of {@code PUT /v1/users/me}, bound loosely so the service can report every invalid field. */
public record UpdateProfileRequest(String displayName, String cityId, BigDecimal radiusKm) {
}
