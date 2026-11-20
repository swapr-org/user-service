package app.swapr.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class InternalTokenFilterTest {

  private static final String TOKEN = "s3cret-internal-token";

  private final InternalTokenFilter filter = new InternalTokenFilter(TOKEN);
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void refusesToStartWithoutConfiguredToken(String configured) {
    assertThatThrownBy(() -> new InternalTokenFilter(configured))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("INTERNAL_TOKEN");
  }

  @Test
  void rejectsInternalRequestWithoutTokenAs401ProblemDetails() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/users");

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("\"status\":401")
        .contains("\"title\":\"Unauthorized\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void rejectsInternalRequestWithWrongTokenAs401() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/users");
    request.addHeader("X-Internal-Token", "not-the-token");

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void rejectsInternalRequestCarryingAuthorizationHeaderAs403EvenWithCorrectToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/users");
    request.addHeader("X-Internal-Token", TOKEN);
    request.addHeader("Authorization", "Bearer user.jwt.token");

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString()).contains("\"status\":403").contains("\"title\":\"Forbidden\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void ignoresRequestsOutsideInternalPaths() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/internals");
    request.addHeader("Authorization", "Bearer user.jwt.token");

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void passesInternalRequestWithCorrectTokenOnAsAuthenticatedInternalCaller() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/users");
    request.addHeader("X-Internal-Token", TOKEN);
    Authentication[] seen = new Authentication[1];
    MockFilterChain capturing = new MockFilterChain(new HttpServlet() {
      @Override
      protected void service(HttpServletRequest req, HttpServletResponse res) {
        seen[0] = SecurityContextHolder.getContext().getAuthentication();
      }
    });

    filter.doFilter(request, response, capturing);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(seen[0]).isNotNull();
    assertThat(seen[0].isAuthenticated()).isTrue();
    assertThat(seen[0].getName()).isEqualTo("internal");
    assertThat(seen[0].getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_INTERNAL");
  }
}
