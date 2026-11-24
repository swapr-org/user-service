package app.swapr.user.dto;

import app.swapr.user.model.Profile;
import java.util.UUID;

/** Another user's public profile: display name and city only. */
public record PublicProfileResponse(UUID id, String displayName, PublicCityResponse city) {

  /** Creates the response for the given complete profile. */
  public static PublicProfileResponse from(Profile profile) {
    return new PublicProfileResponse(profile.id(), profile.displayName(), PublicCityResponse.from(profile.city()));
  }
}
