package org.example.service.controller;

import org.example.api.v1.controller.AuthController;
import org.example.security.JwtAuthenticationFilter;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @SuppressWarnings("unused")
    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockBean
    private JwtUtil jwtUtil;

    @SuppressWarnings("unused")
    @MockBean
    private AuthenticationManager authenticationManager;

    @SuppressWarnings("unused")
    @MockBean
    private UserDetailsService userDetailsService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /auth/login returns JWT token")
    void testLoginReturnsToken() throws Exception {
        UserDetails mockUser = User.withUsername("admin")
                .password("encoded")
                .roles("ADMIN")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        mockUser.getUsername(),
                        mockUser.getPassword(),
                        mockUser.getAuthorities()
                ));

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(mockUser);

        when(jwtUtil.generateToken(any()))
                .thenReturn("fake-jwt-token");

        String authRequest = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }
}
