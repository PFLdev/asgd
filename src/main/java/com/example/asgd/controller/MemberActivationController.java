package com.example.asgd.controller;

import com.example.asgd.dto.MemberActivationRequest;
import com.example.asgd.dto.MemberActivationResponse;
import com.example.asgd.service.MemberActivationExportService;
import com.example.asgd.service.MemberActivationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/member")
public class MemberActivationController {

    @Autowired
    private MemberActivationService memberActivationService;

    @Autowired
    private MemberActivationExportService memberActivationExportService;

    @PostMapping("/activate")
    public MemberActivationResponse activate(@RequestBody MemberActivationRequest request) {
        return memberActivationService.activate(request);
    }

    @GetMapping("/activation-records/export")
    public void exportActivationRecords(
            @RequestParam(required = false) String modelCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activation-records.xlsx\"");
        memberActivationExportService.exportActivationRecords(
                modelCode,
                startTime,
                endTime,
                response.getOutputStream()
        );
    }
}
