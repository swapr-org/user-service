package app.swapr.user.cities;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cities")
class CityController {

  private final CityService cities;

  CityController(CityService cities) {
    this.cities = cities;
  }

  @GetMapping
  List<CityResponse> search(@RequestParam(required = false) String query) {
    return cities.search(query).stream()
        .map(city -> new CityResponse(city.id(), city.name(), city.region(), city.countryCode()))
        .toList();
  }

  record CityResponse(UUID id, String name, String region, String countryCode) {
  }
}
