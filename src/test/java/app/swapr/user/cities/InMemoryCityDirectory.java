package app.swapr.user.cities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** An in-memory city directory that matches prefixes with the same LIKE rules as the SQL one. */
public class InMemoryCityDirectory implements CityDirectory {

  private record Row(City city, String asciiName, String foldedName) {
  }

  private final List<Row> rows = new ArrayList<>();

  /** Adds a city with the ASCII and folded names it is stored with. */
  public InMemoryCityDirectory add(City city, String asciiName, String foldedName) {
    rows.add(new Row(city, asciiName, foldedName));
    return this;
  }

  @Override
  public List<City> searchByPrefix(String prefix, String foldedPrefix) {
    Pattern plain = like(prefix);
    Pattern folded = like(foldedPrefix);
    return rows.stream()
        .filter(row -> plain.matcher(row.city().name().toLowerCase(Locale.ROOT)).matches()
            || plain.matcher(row.asciiName().toLowerCase(Locale.ROOT)).matches()
            || folded.matcher(row.foldedName()).matches())
        .map(Row::city)
        .limit(10)
        .toList();
  }

  @Override
  public Optional<City> find(UUID id) {
    return rows.stream().map(Row::city).filter(city -> city.id().equals(id)).findFirst();
  }

  private static Pattern like(String pattern) {
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '\\' && i + 1 < pattern.length()) {
        regex.append(Pattern.quote(String.valueOf(pattern.charAt(++i))));
      } else if (c == '%') {
        regex.append(".*");
      } else if (c == '_') {
        regex.append('.');
      } else {
        regex.append(Pattern.quote(String.valueOf(c)));
      }
    }
    return Pattern.compile(regex.toString(), Pattern.DOTALL);
  }
}
