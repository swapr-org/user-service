package app.swapr.user.repository;

import app.swapr.user.model.City;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CityRepository extends JpaRepository<City, UUID> {

  @Query(nativeQuery = true, value =
      """
      SELECT * FROM cities
      WHERE lower(name) LIKE :prefix ESCAPE '\\'
         OR lower(ascii_name) LIKE :prefix ESCAPE '\\'
         OR folded_name LIKE :foldedPrefix ESCAPE '\\'
      ORDER BY population DESC, name, id
      LIMIT 10
      """)
  List<City> searchByPrefix(@Param("prefix") String prefix, @Param("foldedPrefix") String foldedPrefix);
}
