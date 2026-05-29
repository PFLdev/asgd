package com.example.asgd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void claimCouponReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "couponId": 10001,
                                  "userId": 123456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.couponId").value(10001))
                .andExpect(jsonPath("$.userId").value(123456));
    }

    @Test
    void claimCouponReturnsAlreadyClaimedForDuplicateUser() throws Exception {
        String requestBody = """
                {
                  "couponId": 10001,
                  "userId": 223456
                }
                """;

        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_CLAIMED"));
    }

    @Test
    void claimCouponReturnsInvalidRequestForMissingCouponId() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 323456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_REQUEST"));
    }

    @Test
    void claimCouponReturnsSoldOut() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "couponId": 10002,
                                  "userId": 423456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }
}
