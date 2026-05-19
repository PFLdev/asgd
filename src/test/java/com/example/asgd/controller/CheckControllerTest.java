package com.example.asgd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsServiceStatus() throws Exception {
        mockMvc.perform(get("/api/check/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("asgd"));
    }

    @Test
    void listItemsReturnsDefaultChecks() throws Exception {
        mockMvc.perform(get("/api/check/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("heap"))
                .andExpect(jsonPath("$[0].name").value("Heap Memory"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void oomEndpointRetainsAllocatedHeapForAnalysis() throws Exception {
        mockMvc.perform(get("/api/check/oom/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retainedMegabytes").value(0));

        mockMvc.perform(get("/api/check/oom").param("mb", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocatedMegabytes").value(1))
                .andExpect(jsonPath("$.retainedMegabytes").value(1))
                .andExpect(jsonPath("$.retainedChunks").value(1));

        mockMvc.perform(get("/api/check/oom/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retainedMegabytes").value(0))
                .andExpect(jsonPath("$.retainedChunks").value(0));
    }
}
