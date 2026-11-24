package app.swapr.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.swapr.user.exception.FieldError;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.model.City;
import app.swapr.user.repository.CityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

  private static final City ZURICH = new City(UUID.randomUUID(), "Zürich", "Zurich", "CH", 47.36667, 8.55);

  @Mock
  private CityRepository repository;

  private CityService cities;

  @BeforeEach
  void setUp() {
    cities = new CityServiceImpl(repository);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "zur       | zur%  | zur%",
      "'  Zür  ' | zür%  | zur%",
      "ZÚR       | zúr%  | zur%",
  })
  void searchesByTrimmedLowercasedPrefixAndItsAccentFoldedForm(String query, String prefix, String folded) {
    when(repository.searchByPrefix(prefix, folded)).thenReturn(List.of(ZURICH));

    assertThat(cities.search(query)).containsExactly(ZURICH);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "z_rich   | z\\_rich%",
      "%rich    | \\%rich%",
      "back\\   | back\\\\%",
  })
  void escapesLikeWildcardsAndBackslash(String query, String pattern) {
    cities.search(query);

    verify(repository).searchByPrefix(pattern, pattern);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"z", "  z  "})
  void rejectsQueriesShorterThanTwoCharactersWithoutSearching(String query) {
    assertThatThrownBy(() -> cities.search(query))
        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.errors())
            .extracting(FieldError::field)
            .containsExactly("query"));
    verifyNoInteractions(repository);
  }

  @Test
  void findReturnsTheStoredCity() {
    when(repository.findById(ZURICH.id())).thenReturn(Optional.of(ZURICH));

    assertThat(cities.find(ZURICH.id())).contains(ZURICH);
  }
}
