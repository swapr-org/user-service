package app.swapr.user.dto;

import java.util.UUID;

/** Another user's public profile: display name and city only. */
public record PublicProfileResponse(UUID id, String displayName, PublicCityResponse city) {

  /** Creates the response for the given public profile view. */
  public static PublicProfileResponse from(PublicProfileView profile) {
    return new PublicProfileResponse(profile.id(), profile.displayName(), PublicCityResponse.from(profile.city()));
  }
}
