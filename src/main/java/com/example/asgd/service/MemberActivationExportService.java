package com.example.asgd.service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

public interface MemberActivationExportService {

    void exportActivationRecords(
            String modelCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            OutputStream outputStream
    ) throws IOException;
}
