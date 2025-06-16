package com.kakaobase.snsapp.global.security.jwt;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtTokenValidator jwtTokenValidator;
    private final CustomUserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 필터를 적용하지 않을 전체 경로 목록 (풀 패스 + 와일드카드)
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/auth/tokens",
            "/api/auth/tokens/refresh",
            "/api/users",
            "/api/users/email/verification-requests",
            "/api/users/email/verification",
            "/api/actuator/health",
            "/actuator/health",
            "/api/swagger-ui/**",
            "/api/v3/api-docs/**"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.debug("▶ shouldNotFilter URI = {}", uri);

        // POST 회원가입 예외
        if ("POST".equals(request.getMethod()) && uri.equals("/api/users")) {
            return true;
        }

        // 기타 excluded 경로
        for (String pattern : EXCLUDED_PATHS) {
            if (pathMatcher.match(pattern, uri)) {
                log.debug("   → excluded by pattern {}", pattern);
                return true;
            }
        }
        return false;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JwtAuthenticationFilter 실행 - URI: {} Method: {}", request.getRequestURI(), request.getMethod());
        String token = jwtUtil.resolveToken(request);
        log.debug("추출된 토큰: {}", token != null ? "존재함(length=" + token.length() + ")" : "없음");

        if (StringUtils.hasText(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtTokenValidator.validateToken(token)) {
                    String userId = jwtUtil.getSubject(token);
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(userId);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT 인증 성공: {}", userId);
                }
            } catch (CustomException e) {
                log.error("JWT 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
