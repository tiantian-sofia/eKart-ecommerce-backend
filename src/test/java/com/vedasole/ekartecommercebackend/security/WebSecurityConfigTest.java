package com.vedasole.ekartecommercebackend.security;

import com.vedasole.ekartecommercebackend.utility.TestApplicationInitializer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestApplicationInitializer testApplicationInitializer;

    @Value("${jwt.secret.key}")
    private String jwtSecretKey;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Creates a JWT token with a null subject to simulate the scenario
     * where extractUsername returns null.
     */
    private String createTokenWithNullSubject() {
        return Jwts.builder()
                .setSubject(null)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void whenAccessProtectedEndpointWithNullSubjectToken_thenUnauthorized() throws Exception {
        String badToken = createTokenWithNullSubject();
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + badToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessProtectedEndpointWithMalformedToken_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessProtectedPostWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessPublicUrl_thenOk() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andDo(result -> log.debug("whenAccessPublicUrl_thenOk result: {}", result.getResponse().getContentAsString()))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessProtectedUrlWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessGeneralProtectedUrlWithAuth_thenOk() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-token")
                .header("Authorization", "Bearer " + testApplicationInitializer.getUserToken()))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessAdminProtectedUrlWithAuth_thenOk() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/3")
                        .header("Authorization", "Bearer " + testApplicationInitializer.getAdminToken()))
                .andExpectAll(
                        jsonPath("$.message").value("Customer deleted successfully"),
                        jsonPath("$.success").value(true),
                        status().isOk()
                );
    }

}