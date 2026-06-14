package com.vedasole.ekartecommercebackend.security;

import com.vedasole.ekartecommercebackend.utility.TestApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JWTAuthenticationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestApplicationInitializer testApplicationInitializer;

    @Test
    void whenMalformedToken_thenReturn401AndNotReachController() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer completely.invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Test\",\"categoryImage\":\"img.png\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTamperedToken_thenReturn401AndNotReachController() throws Exception {
        // Take a valid token and tamper with it
        String validToken = testApplicationInitializer.getUserToken();
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        mockMvc.perform(put("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Hacked\",\"lastName\":\"User\",\"email\":\"hacked@test.com\",\"phoneNumber\":\"1234567890\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenEmptyBearerToken_thenReturn401AndNotReachController() throws Exception {
        // "Bearer " with length > 7 but garbage content
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Test\",\"categoryImage\":\"img.png\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenValidToken_thenFilterAllowsRequestThrough() throws Exception {
        String validToken = testApplicationInitializer.getUserToken();

        // A valid token should pass the JWT filter and reach the controller.
        // We verify the response is NOT 401, proving the filter did not block it.
        mockMvc.perform(put("/api/v1/customers/2")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Normal\",\"lastName\":\"User\",\"email\":\"normal-user@ekart.com\",\"phoneNumber\":\"1234567890\"}"))
                .andExpect(status().isOk());
    }
}
