package app.swapr.user.profiles;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProfileRepository extends JpaRepository<Profile, UUID> {

  @Modifying
  @Query(nativeQuery = true,
      value = "INSERT INTO profiles (id, radius_km) VALUES (:id, :radiusKm) ON CONFLICT (id) DO NOTHING")
  void insertIfAbsent(@Param("id") UUID id, @Param("radiusKm") int radiusKm);

  @Modifying
  @Query("DELETE FROM Profile p WHERE p.id = :id")
  void deleteProfile(@Param("id") UUID id);
}
