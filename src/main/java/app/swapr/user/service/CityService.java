package app.swapr.user.service;

import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Looks up cities from the GeoNames gazetteer. */
public interface CityService {

  /** Returns at most 10 cities whose name starts with the query, ignoring case and accents, largest first. */
  List<City> search(String query) throws ValidationException;

  /** Returns the city with the given ID, if it exists. */
  Optional<City> find(UUID id);
}
