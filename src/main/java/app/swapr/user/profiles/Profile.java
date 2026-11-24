package app.swapr.user.profiles;

import app.swapr.user.cities.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
class Profile {

  @Id
  private UUID id;

  @Column(name = "display_name")
  private String displayName;

  @ManyToOne
  @JoinColumn(name = "city_id")
  private City city;

  @Column(name = "radius_km")
  private int radiusKm;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  protected Profile() {
  }

  Profile(UUID id, String displayName, City city, int radiusKm, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.displayName = displayName;
    this.city = city;
    this.radiusKm = radiusKm;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  Profile replaced(String displayName, City city, int radiusKm, Instant now) {
    return new Profile(id, displayName, city, radiusKm, createdAt, now);
  }

  ProfileView view() {
    return new ProfileView(id, displayName, radiusKm, city);
  }
}
