package app.swapr.user.dto;

import app.swapr.user.model.City;
import app.swapr.user.model.Profile;
import java.util.UUID;

/** A user's own profile as the service returns it. */
public record ProfileView(UUID id, String displayName, int radiusKm, City city) {

  /** Creates the view of the given profile. */
  public static ProfileView from(Profile profile) {
    return new ProfileView(profile.id(), profile.displayName(), profile.radiusKm(), profile.city());
  }
}
