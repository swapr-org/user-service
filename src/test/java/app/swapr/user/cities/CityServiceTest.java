package app.swapr.user.cities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.swapr.user.ServiceTest;
import app.swapr.user.errors.FieldError;
import app.swapr.user.errors.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceTest
class CityServiceTest {

  @Autowired
  private CityService cities;

  @ParameterizedTest
  @ValueSource(strings = {"zur", "ZUR", "zür", "Zue", "  zur  "})
  void findsZurichByPrefixIgnoringCaseAndAccents(String query) {
    List<City> result = cities.search(query);

    assertThat(result)
        .anySatisfy(city -> {
          assertThat(city.name()).isEqualTo("Zürich");
          assertThat(city.region()).isEqualTo("Zurich");
          assertThat(city.countryCode()).isEqualTo("CH");
        });
  }

  @Test
  void ordersMatchesByPopulationAndReturnsAtMostTen() {
    List<City> result = cities.search("lon");

    assertThat(result).hasSize(10);
    assertThat(result).extracting(City::name).startsWith("London", "Longyan", "Londrina");
    assertThat(result.getFirst().countryCode()).isEqualTo("GB");
  }

  @Test
  void matchesOnlyFromTheStartOfTheName() {
    assertThat(cities.search("urich")).extracting(City::name).doesNotContain("Zürich");
  }

  @ParameterizedTest
  @ValueSource(strings = {"z_rich", "%rich", "zu%"})
  void treatsWildcardCharactersLiterally(String query) {
    assertThat(cities.search(query)).isEmpty();
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
}
