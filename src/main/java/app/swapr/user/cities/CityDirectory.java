package app.swapr.user.cities;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The stored GeoNames cities that {@link CityService} reads. */
public interface CityDirectory {

  /**
   * Returns at most 10 cities, most populous first, whose lowercased name or ASCII name matches the
   * {@code LIKE} pattern {@code prefix}, or whose folded name matches {@code foldedPrefix}. Both patterns
   * escape wildcards with a backslash.
   */
  List<City> searchByPrefix(String prefix, String foldedPrefix);

  /** Returns the city with the given ID, if it exists. */
  Optional<City> find(UUID id);
}
