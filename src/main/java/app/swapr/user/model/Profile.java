package app.swapr.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A user's profile, keyed by their Supabase user ID. */
@Entity
@Table(name = "profiles")
public class Profile {

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

  Profile() {
  }

  /** Creates a profile with the given details. */
  public Profile(UUID id, String displayName, City city, int radiusKm, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.displayName = displayName;
    this.city = city;
    this.radiusKm = radiusKm;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Returns a copy with the name, city and radius replaced, updated at {@code now}. */
  public Profile replaced(String displayName, City city, int radiusKm, Instant now) {
    return new Profile(id, displayName, city, radiusKm, createdAt, now);
  }

  /** Returns whether the profile has a display name and a city. */
  public boolean isComplete() {
    return displayName != null && city != null;
  }

  /** Returns the user ID. */
  public UUID id() {
    return id;
  }

  /** Returns the display name, or null until it is set. */
  public String displayName() {
    return displayName;
  }

  /** Returns the chosen city, or null until it is set. */
  public City city() {
    return city;
  }

  /** Returns the search radius in kilometres. */
  public int radiusKm() {
    return radiusKm;
  }
}
