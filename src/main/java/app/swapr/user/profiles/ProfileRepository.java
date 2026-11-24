package app.swapr.user.profiles;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProfileRepository extends JpaRepository<Profile, UUID> {

  @Modifying
  @Query(value = "INSERT INTO profiles (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
  void insertDefaultIfAbsent(@Param("id") UUID id);

  @Modifying
  @Query("DELETE FROM Profile p WHERE p.id = :id")
  void deleteProfile(@Param("id") UUID id);
}
