package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.Shift;
import com.tea.time_mileage_tracker.model.StoreVisit;
import com.tea.time_mileage_tracker.repository.ShiftRepository;
import com.tea.time_mileage_tracker.repository.StoreVisitRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final ZoneId REPORT_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(REPORT_ZONE);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a").withZone(REPORT_ZONE);

    private final ShiftRepository shiftRepository;
    private final StoreVisitRepository storeVisitRepository;

    public ReportController(ShiftRepository shiftRepository, StoreVisitRepository storeVisitRepository) {
        this.shiftRepository = shiftRepository;
        this.storeVisitRepository = storeVisitRepository;
    }

    // Wraps a value in quotes and escapes inner quotes, so commas and line breaks
    // inside a note don't break the spreadsheet columns.
    private String cell(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String time(Instant t) {
        return t == null ? "" : TIME_FMT.format(t);
    }

    private String lunch(Shift s) {
        if (s.getLunchStartTime() == null) return "";
        if (s.getLunchEndTime() == null) return time(s.getLunchStartTime()) + " to ?";
        return time(s.getLunchStartTime()) + " to " + time(s.getLunchEndTime());
    }

    private String hours(Double h) {
        return h == null ? "" : String.format("%.2f", h);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String start, @RequestParam String end) {
        Instant startInstant = LocalDate.parse(start).atStartOfDay(REPORT_ZONE).toInstant();
        Instant endInstant = LocalDate.parse(end).atTime(23, 59, 59).atZone(REPORT_ZONE).toInstant();

        List<Shift> shifts = shiftRepository.findByClockInTimeBetween(startInstant, endInstant)
                .stream()
                .sorted(Comparator.comparing(Shift::getClockInTime))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // lets Excel read the file as UTF-8
        sb.append("Date,Clock In,Clock Out,Lunch,Total Hours,Stop #,Store,Notes\n");

        double grandTotal = 0;

        for (Shift shift : shifts) {
            List<StoreVisit> visits = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shift.getId());
            if (shift.getTotalHours() != null) grandTotal += shift.getTotalHours();

            // Shift info goes on the first row only. Following rows for the same day
            // leave those columns blank, so summing Total Hours stays accurate.
            String shiftCols = String.join(",",
                    cell(DATE_FMT.format(shift.getClockInTime())),
                    cell(time(shift.getClockInTime())),
                    cell(time(shift.getClockOutTime())),
                    cell(lunch(shift)),
                    hours(shift.getTotalHours()));
            String blankShiftCols = ",,,,";

            if (visits.isEmpty()) {
                sb.append(shiftCols).append(",,,\n");
                continue;
            }

            for (int i = 0; i < visits.size(); i++) {
                StoreVisit v = visits.get(i);
                sb.append(i == 0 ? shiftCols : blankShiftCols).append(",");
                sb.append(v.getSequenceOrder()).append(",");
                sb.append(cell(v.getStore() != null ? v.getStore().getName() : "")).append(",");
                sb.append(cell(v.getNote())).append("\n");
            }
        }

        sb.append("\n");
        sb.append(",,,").append(cell("Period total")).append(",").append(hours(grandTotal)).append(",,,\n");

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "time_report_" + start + "_to_" + end + ".csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}