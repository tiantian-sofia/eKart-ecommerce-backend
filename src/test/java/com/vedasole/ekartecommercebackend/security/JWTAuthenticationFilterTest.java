package com.vedasole.ekartecommercebackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JWTAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthHeader_shouldContinueFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternal_validToken_shouldSetAuthenticationAndContinue() throws Exception {
        String token = "valid.jwt.token";
        String email = "user@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(email);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternal_nullUsername_shouldSend401AndNotContinueFilterChain() throws Exception {
        String token = "invalid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_extractUsernameThrowsException_shouldSend401AndNotContinue() throws Exception {
        String token = "malformed.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new io.jsonwebtoken.MalformedJwtException("malformed"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_invalidToken_shouldNotSetAuthButContinue() throws Exception {
        String token = "invalid.jwt.token";
        String email = "user@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(email);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_bearerWithoutToken_shouldContinueFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternal_existingAuthentication_shouldContinueFilterChain() throws Exception {
        // Pre-set authentication in SecurityContext
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String token = "valid.jwt.token";
        String email = "user@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(email);

        filter.doFilterInternal(request, response, filterChain);

        // Should continue without error — existing auth is preserved
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }
}
