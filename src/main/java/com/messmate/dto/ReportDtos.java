package com.messmate.dto;

import com.messmate.entity.MealType;

import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record MealAvg(Long mealId, LocalDate date, MealType mealType, double average, long count) {}

    public record DayAvg(LocalDate date, double average, long count) {}

    public record WeeklyAverages(LocalDate weekStart, LocalDate weekEnd, List<MealAvg> perMeal, List<DayAvg> perDay) {}

    /** expected = totalResidents - optedOut. */
    public record HeadcountRow(Long mealId, MealType mealType, long totalResidents, long optedOut, long expected) {}

    public record Dashboard(long residents, long meals, long menuItems, long feedbackEntries, Double overallAverage) {}
}
