package app.swapr.user.dto;

import java.util.UUID;

/** The user's own profile as {@code /v1/users/me} returns it. */
public record ProfileResponse(UUID id, String displayName, int radiusKm, CityResponse city) {

  /** Creates the response for the given profile view. */
  public static ProfileResponse from(ProfileView profile) {
    CityResponse city = profile.city() == null ? null : CityResponse.from(profile.city());
    return new ProfileResponse(profile.id(), profile.displayName(), profile.radiusKm(), city);
  }
}
