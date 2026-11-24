package app.swapr.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class InternalTokenFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Internal-Token";

  private final byte[] token;

  InternalTokenFilter(String token) throws IllegalStateException {
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("INTERNAL_TOKEN must be set to a non-blank value");
    }
    this.token = token.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !path.equals("/internal") && !path.startsWith("/internal/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!matches(request.getHeader(HEADER))) {
      ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Missing or invalid X-Internal-Token header.",
          request.getRequestURI());
      return;
    }
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
      ProblemResponses.write(response, HttpStatus.FORBIDDEN, "Internal endpoints do not accept user credentials.",
          request.getRequestURI());
      return;
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("internal", null,
        AuthorityUtils.createAuthorityList("ROLE_INTERNAL")));
    SecurityContextHolder.setContext(context);
    chain.doFilter(request, response);
  }

  private boolean matches(String presented) {
    return presented != null && MessageDigest.isEqual(token, presented.getBytes(StandardCharsets.UTF_8));
  }
}
