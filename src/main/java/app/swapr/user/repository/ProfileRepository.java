package app.swapr.user.repository;

import app.swapr.user.model.Profile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The stored user profiles. */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  /** Inserts a profile with only an ID and radius, doing nothing when one already exists. */
  @Modifying
  @Query(nativeQuery = true,
      value = "INSERT INTO profiles (id, radius_km) VALUES (:id, :radiusKm) ON CONFLICT (id) DO NOTHING")
  void insertIfAbsent(@Param("id") UUID id, @Param("radiusKm") int radiusKm);

  /** Deletes the profile with the given ID, doing nothing when none exists. */
  @Modifying
  @Query("DELETE FROM Profile p WHERE p.id = :id")
  void deleteProfile(@Param("id") UUID id);
}
