package app.swapr.user.profiles;

import java.math.BigDecimal;

record UpdateProfileCommand(String displayName, String cityId, BigDecimal radiusKm) {
}
