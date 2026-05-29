package com.example.asgd.controller;

import com.example.asgd.dto.MemberActiveDeviceAccessResponse;
import com.example.asgd.dto.MemberActiveDeviceAccessStatus;
import com.example.asgd.service.MemberActiveDeviceLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberActiveDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberActiveDeviceLimitService memberActiveDeviceLimitService;

    @Test
    void memberAccessEndpointAllowsRequestWhenDeviceLimitAllows() throws Exception {
        when(memberActiveDeviceLimitService.acquire(any()))
                .thenReturn(MemberActiveDeviceAccessResponse.allowed("Member feature access allowed"));

        mockMvc.perform(post("/api/member/active-device/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-001",
                                  "deviceId": "device-001",
                                  "sessionId": "session-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALLOWED"))
                .andExpect(jsonPath("$.allowed").value(true));

        ArgumentCaptor<com.example.asgd.dto.MemberActiveDeviceAccessRequest> captor =
                ArgumentCaptor.forClass(com.example.asgd.dto.MemberActiveDeviceAccessRequest.class);
        verify(memberActiveDeviceLimitService).acquire(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("user-001");
        assertThat(captor.getValue().deviceId()).isEqualTo("device-001");
    }

    @Test
    void memberAccessEndpointReturnsLimitExceededWithoutRunningMemberFeature() throws Exception {
        when(memberActiveDeviceLimitService.acquire(any()))
                .thenReturn(MemberActiveDeviceAccessResponse.limitExceeded(3));

        mockMvc.perform(post("/api/member/active-device/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-001",
                                  "deviceId": "device-004",
                                  "sessionId": "session-004"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.message").value("Current account already has 3 active member devices"));
    }

    @Test
    void memberActivationLoginLikeFlowDoesNotUseActiveDeviceLimit() throws Exception {
        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "active-limit-unaffected-device",
                                  "modelCode": "EDU-PAD-2026",
                                  "userId": "active-limit-user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void memberAccessEndpointCanReturnTemporaryUnavailableForFailClosedRedisErrors() throws Exception {
        when(memberActiveDeviceLimitService.acquire(any()))
                .thenReturn(MemberActiveDeviceAccessResponse.temporarilyUnavailable());

        mockMvc.perform(post("/api/member/active-device/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-001",
                                  "deviceId": "device-001",
                                  "sessionId": "session-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MemberActiveDeviceAccessStatus.TEMPORARILY_UNAVAILABLE.name()))
                .andExpect(jsonPath("$.allowed").value(false));
    }
}
