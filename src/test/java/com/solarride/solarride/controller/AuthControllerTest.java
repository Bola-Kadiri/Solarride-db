package com.solarride.solarride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarride.solarride.dto.request.CustomerRegisterRequest;
import com.solarride.solarride.dto.response.UserResponse;
import com.solarride.solarride.service.onboarding.CustomerOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CustomerOnboardingService customerOnboardingService;

    @MockBean
    com.solarride.solarride.service.auth.AuthService authService;

    @MockBean
    com.solarride.solarride.service.auth.JwtService jwtService;

    @MockBean
    com.solarride.solarride.security.SolarRideUserDetailsService userDetailsService;

    @Test
    void registerCustomer_validRequest_returns201() throws Exception {
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "test@example.com", "+2348012345678",
                "John", "Doe", "password123");

        UserResponse resp = new UserResponse(
                UUID.randomUUID(), "test@example.com", "+2348012345678",
                "John", "Doe", "CUSTOMER", "ACTIVE", false, false);

        when(customerOnboardingService.register(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerCustomer_invalidEmail_returns400() throws Exception {
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "not-an-email", "+2348012345678", "John", "Doe", "password123");

        mockMvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}