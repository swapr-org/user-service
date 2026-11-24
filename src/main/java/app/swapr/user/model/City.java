package app.swapr.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A GeoNames city whose centre point is the location of every profile that chooses it. */
@Entity
@Table(name = "cities")
public class City {

  @Id
  private UUID id;

  private String name;

  private String region;

  @Column(name = "country_code")
  private String countryCode;

  City() {
  }

  /** Creates a city with the given details. */
  public City(UUID id, String name, String region, String countryCode) {
    this.id = id;
    this.name = name;
    this.region = region;
    this.countryCode = countryCode;
  }

  /** Returns the city's ID. */
  public UUID id() {
    return id;
  }

  /** Returns the city's name. */
  public String name() {
    return name;
  }

  /** Returns the first-level region name, or null when GeoNames has none. */
  public String region() {
    return region;
  }

  /** Returns the two-letter country code. */
  public String countryCode() {
    return countryCode;
  }
}
