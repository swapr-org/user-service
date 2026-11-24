package app.swapr.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.swapr.user.exception.FieldError;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import app.swapr.user.repository.InMemoryCityDirectory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CityServiceTest {

  private final CityService cities = new CityService(new InMemoryCityDirectory()
      .add(city("Zürich"), "Zuerich", "zurich")
      .add(city("Port_Town"), "Port_Town", "port_town")
      .add(city("Back\\slash"), "Back\\slash", "back\\slash"));

  @ParameterizedTest
  @ValueSource(strings = {"zur", "ZÚR", "Zue", "  zur  "})
  void trimsLowercasesAndFoldsTheQuery(String query) {
    assertThat(cities.search(query)).extracting(City::name).containsExactly("Zürich");
  }

  @ParameterizedTest
  @ValueSource(strings = {"z_rich", "%rich", "port%own"})
  void treatsWildcardsLiterally(String query) {
    assertThat(cities.search(query)).isEmpty();
  }

  @Test
  void treatsBackslashLiterally() {
    assertThat(cities.search("back\\")).extracting(City::name).containsExactly("Back\\slash");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"z", "  z  "})
  void rejectsQueriesShorterThanTwoCharacters(String query) {
    assertThatThrownBy(() -> cities.search(query))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactly("query"));
  }

  private static City city(String name) {
    return new City(UUID.randomUUID(), name, null, "CH", 0, 0);
  }
}
