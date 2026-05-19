package com.example.asgd.service.impl;

import com.example.asgd.dao.MemberActivationMapper;
import com.example.asgd.entity.MemberActivationRecord;
import com.example.asgd.service.MemberActivationExportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MemberActivationExportServiceImpl implements MemberActivationExportService {

    private static final int PAGE_SIZE = 500;
    private static final int MAX_EXPORT_ROWS = 100_000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberActivationMapper memberActivationMapper;

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        private int count = 1;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("export-worker-" + count++);
            return thread;
        }
    };

    private static final ThreadPoolExecutor EXECUTOR_POOL = new ThreadPoolExecutor(
            4,
            10,
            5,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public MemberActivationExportServiceImpl(MemberActivationMapper memberActivationMapper) {
        this.memberActivationMapper = memberActivationMapper;
    }

    @Override
    public void exportActivationRecords(
            String modelCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            OutputStream outputStream
    ) throws IOException {
        if (endTime.isBefore(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must not be before startTime");
        }

        List<MemberActivationRecord> rows = new ArrayList<>();
        long lastId = 0L;
        while (rows.size() <= MAX_EXPORT_ROWS) {
            int limit = Math.min(PAGE_SIZE, MAX_EXPORT_ROWS + 1 - rows.size());
            List<MemberActivationRecord> page = memberActivationMapper.listActivationRecordsForExport(
                    blankToNull(modelCode),
                    startTime,
                    endTime,
                    lastId,
                    limit
            );
            if (page.isEmpty()) {
                break;
            }
            rows.addAll(page);
            if (rows.size() > MAX_EXPORT_ROWS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Export row count exceeds " + MAX_EXPORT_ROWS
                );
            }
            lastId = page.get(page.size() - 1).id();
        }

        writeWorkbook(rows, outputStream);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void writeWorkbook(List<MemberActivationRecord> rows, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeEntry(zipOutputStream, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            writeEntry(zipOutputStream, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            writeEntry(zipOutputStream, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="activation-records" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """);
            writeEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            writeEntry(zipOutputStream, "xl/worksheets/sheet1.xml", buildSheet(rows));
        }
    }

    private static String buildSheet(List<MemberActivationRecord> rows) {
        StringBuilder sheet = new StringBuilder();
        sheet.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        appendRow(sheet, 1, List.of(
                "\u8bb0\u5f55ID",
                "\u8bbe\u5907ID",
                "\u578b\u53f7\u7f16\u7801",
                "\u7528\u6237ID",
                "\u6fc0\u6d3b\u65f6\u95f4"
        ));
        int rowIndex = 2;
        for (MemberActivationRecord row : rows) {
            appendRow(sheet, rowIndex++, List.of(
                    String.valueOf(row.id()),
                    row.deviceId(),
                    row.modelCode(),
                    row.userId() == null ? "" : row.userId(),
                    DATE_TIME_FORMATTER.format(row.activatedAt())
            ));
        }
        sheet.append("""
                  </sheetData>
                </worksheet>
                """);
        return sheet.toString();
    }

    private static void appendRow(StringBuilder sheet, int rowIndex, List<String> values) {
        sheet.append("    <row r=\"").append(rowIndex).append("\">");
        for (String value : values) {
            sheet.append("<c t=\"inlineStr\"><is><t>")
                    .append(escapeXml(value))
                    .append("</t></is></c>");
        }
        sheet.append("</row>\n");
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
}
