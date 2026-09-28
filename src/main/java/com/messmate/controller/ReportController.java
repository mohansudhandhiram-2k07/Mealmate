package com.messmate.controller;

import com.messmate.dto.ReportDtos.*;
import com.messmate.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    /** Average rating per meal and per day for a week. */
    @GetMapping("/averages")
    public WeeklyAverages averages(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.averages(weekStart);
    }

    /** Meals rated below the threshold (default 3.0) in a given week. */
    @GetMapping("/low-rated")
    public List<MealAvg> lowRated(@RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                  @RequestParam(defaultValue = "3.0") double threshold) {
        return service.lowRated(weekStart, threshold);
    }

    /** Headcount for a date: residents minus opted-out, per meal. */
    @GetMapping("/headcount")
    public List<HeadcountRow> headcount(@RequestParam
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.headcount(date);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        return service.dashboard();
    }
}
