package app.swapr.user.dto;

import app.swapr.user.model.City;

/** A city on a public profile, without its ID or coordinates. */
public record PublicCityResponse(String name, String region, String countryCode) {

  /** Creates the response for the given city. */
  public static PublicCityResponse from(City city) {
    return new PublicCityResponse(city.name(), city.region(), city.countryCode());
  }
}
