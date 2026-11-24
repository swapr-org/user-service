package app.swapr.user.controller;

import app.swapr.user.dto.CityResponse;
import app.swapr.user.exception.ValidationException;
import app.swapr.user.service.CityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cities")
class CityController {

  private final CityService cityService;

  CityController(CityService cityService) {
    this.cityService = cityService;
  }

  @GetMapping
  List<CityResponse> search(@RequestParam(required = false) String query) throws ValidationException {
    return cityService.search(query);
  }
}
