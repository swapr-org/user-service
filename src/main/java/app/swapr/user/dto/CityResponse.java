package app.swapr.user.dto;

import app.swapr.user.model.City;
import java.util.UUID;

/** A city as city search and the user's own profile return it, without coordinates. */
public record CityResponse(UUID id, String name, String region, String countryCode) {

  /** Creates the response for the given city. */
  public static CityResponse from(City city) {
    return new CityResponse(city.id(), city.name(), city.region(), city.countryCode());
  }
}
