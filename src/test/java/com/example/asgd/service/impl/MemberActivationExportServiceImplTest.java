package com.example.asgd.service.impl;

import com.example.asgd.dao.MemberActivationMapper;
import com.example.asgd.entity.MemberActivationRecord;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberActivationExportServiceImplTest {

    @Test
    void exportRejectsWhenResultExceedsMaximumRows() {
        MemberActivationExportServiceImpl service = new MemberActivationExportServiceImpl(
                new FakeMemberActivationMapper(100_001)
        );

        assertThatThrownBy(() -> service.exportActivationRecords(
                "EDU-PAD-2026",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                new ByteArrayOutputStream()
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Export row count exceeds 100000");
    }

    private static class FakeMemberActivationMapper implements MemberActivationMapper {

        private final int rowCount;

        private FakeMemberActivationMapper(int rowCount) {
            this.rowCount = rowCount;
        }

        @Override
        public int existsEducationModel(String modelCode) {
            return 0;
        }

        @Override
        public int existsActivation(String deviceId) {
            return 0;
        }

        @Override
        public int insertActivation(String deviceId, String modelCode, String userId, LocalDateTime activatedAt) {
            return 0;
        }

        @Override
        public List<MemberActivationRecord> listActivationRecordsForExport(
                String modelCode,
                LocalDateTime startTime,
                LocalDateTime endTime,
                long lastId,
                int limit
        ) {
            List<MemberActivationRecord> rows = new ArrayList<>();
            long nextId = lastId + 1;
            while (nextId <= rowCount && rows.size() < limit) {
                rows.add(new MemberActivationRecord(
                        nextId,
                        "device-" + nextId,
                        "EDU-PAD-2026",
                        "user-" + nextId,
                        startTime
                ));
                nextId++;
            }
            return rows;
        }
    }
}
