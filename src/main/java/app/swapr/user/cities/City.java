package app.swapr.user.cities;

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

  private double latitude;

  private double longitude;

  City() {
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

  /** Returns the latitude of the city's centre. */
  public double latitude() {
    return latitude;
  }

  /** Returns the longitude of the city's centre. */
  public double longitude() {
    return longitude;
  }
}
