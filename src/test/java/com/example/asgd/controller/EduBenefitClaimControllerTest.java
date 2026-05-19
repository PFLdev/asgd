package com.example.asgd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EduBenefitClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusReturnsNotClaimedForEligibleDevice() throws Exception {
        mockMvc.perform(get("/api/edu/member/claim/status")
                        .param("activityId", "10001")
                        .param("deviceId", "edu-device-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CLAIMED"))
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    void claimGrantsBenefitForEligibleDevice() throws Exception {
        mockMvc.perform(post("/api/edu/member/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": 10001,
                                  "deviceId": "edu-device-002",
                                  "miAccountId": 123456,
                                  "requestId": "request-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.grantStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.grantOrderNo").isNotEmpty());
    }

    @Test
    void claimReturnsExistingSuccessWhenSameDeviceUsesAnotherAccount() throws Exception {
        mockMvc.perform(post("/api/edu/member/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": 10001,
                                  "deviceId": "edu-device-003",
                                  "miAccountId": 123456,
                                  "requestId": "request-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/edu/member/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": 10001,
                                  "deviceId": "edu-device-003",
                                  "miAccountId": 999999,
                                  "requestId": "request-003"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Device benefit already claimed"));
    }

    @Test
    void claimRejectsDeviceOutsideWhitelist() throws Exception {
        mockMvc.perform(post("/api/edu/member/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId": 10001,
                                  "deviceId": "normal-device-001",
                                  "miAccountId": 123456,
                                  "requestId": "request-004"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.grantStatus").value("INIT"));
    }
}
