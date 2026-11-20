package app.swapr.user.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

final class ProblemResponses {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private ProblemResponses() {
  }

  static void write(HttpServletResponse response, HttpStatus status, String detail, String instance)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", status.getReasonPhrase());
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("instance", instance);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    MAPPER.writeValue(response.getOutputStream(), body);
  }
}
