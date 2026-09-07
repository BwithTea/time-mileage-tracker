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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final ZoneId REPORT_ZONE = ZoneId.of("America/New_York");

    private final ShiftRepository shiftRepository;
    private final StoreVisitRepository storeVisitRepository;

    public ReportController(ShiftRepository shiftRepository, StoreVisitRepository storeVisitRepository) {
        this.shiftRepository = shiftRepository;
        this.storeVisitRepository = storeVisitRepository;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String start, @RequestParam String end) {
        Instant startInstant = LocalDate.parse(start).atStartOfDay(REPORT_ZONE).toInstant();
        Instant endInstant = LocalDate.parse(end).atTime(23, 59, 59).atZone(REPORT_ZONE).toInstant();
        List<Shift> shifts = shiftRepository.findByClockInTimeBetween(startInstant, endInstant);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(REPORT_ZONE);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a").withZone(REPORT_ZONE);

        StringBuilder sb = new StringBuilder();
        sb.append("Date,Clock In,Clock Out,Total Hours,Stores Visited,Total Miles\n");

        for (Shift shift : shifts) {
            List<StoreVisit> visits = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shift.getId());
            String storeNames = visits.stream()
                    .map(v -> v.getStore().getName())
                    .collect(Collectors.joining(" -> "));

            sb.append(dateFmt.format(shift.getClockInTime())).append(",");
            sb.append(timeFmt.format(shift.getClockInTime())).append(",");
            sb.append(shift.getClockOutTime() != null ? timeFmt.format(shift.getClockOutTime()) : "").append(",");
            sb.append(shift.getTotalHours() != null ? shift.getTotalHours() : "").append(",");
            sb.append("\"").append(storeNames).append("\",");
            sb.append(shift.getTotalMiles() != null ? shift.getTotalMiles() : "").append("\n");
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "time_report.csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}