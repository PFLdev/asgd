package com.example.asgd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberActivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activateReturnsSuccessForEducationModel() throws Exception {
        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "controller-device-001",
                                  "modelCode": "EDU-PAD-2026",
                                  "userId": "user-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Activation successful"));
    }

    @Test
    void activateRejectsDuplicateDevice() throws Exception {
        String requestBody = """
                {
                  "deviceId": "controller-device-002",
                  "modelCode": "EDU-PAD-2026",
                  "userId": "user-002"
                }
                """;

        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_ACTIVATED"));
    }

    @Test
    void activateRejectsNonEducationModel() throws Exception {
        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "controller-device-003",
                                  "modelCode": "NORMAL-PAD-2026",
                                  "userId": "user-003"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MODEL_NOT_ELIGIBLE"));
    }

    @Test
    void exportActivationRecordsReturnsXlsxWithMatchingRows() throws Exception {
        mockMvc.perform(post("/api/member/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "export-device-001",
                                  "modelCode": "EDU-PAD-2026",
                                  "userId": "export-user-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        byte[] content = mockMvc.perform(get("/api/member/activation-records/export")
                        .param("modelCode", "EDU-PAD-2026")
                        .param("startTime", "2026-01-01T00:00:00")
                        .param("endTime", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"activation-records.xlsx\""))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(content).startsWith(new byte[]{'P', 'K'});
        String worksheetXml = readZipEntry(content, "xl/worksheets/sheet1.xml");
        assertThat(worksheetXml).contains("export-device-001");
        assertThat(worksheetXml).contains("EDU-PAD-2026");
        assertThat(worksheetXml).contains("export-user-001");
        assertThat(worksheetXml).contains("\u6fc0\u6d3b\u65f6\u95f4");
    }

    private static String readZipEntry(byte[] content, String entryName) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalArgumentException("Missing zip entry: " + entryName);
    }
}
