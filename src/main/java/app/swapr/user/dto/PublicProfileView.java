package app.swapr.user.dto;

import app.swapr.user.model.City;
import app.swapr.user.model.Profile;
import java.util.UUID;

/** The part of a complete profile that other users may see, as the service returns it. */
public record PublicProfileView(UUID id, String displayName, City city) {

  /** Creates the public view of the given profile. */
  public static PublicProfileView from(Profile profile) {
    return new PublicProfileView(profile.id(), profile.displayName(), profile.city());
  }
}
