package com.messmate.service;

import com.messmate.dto.ReportDtos.*;
import com.messmate.entity.Meal;
import com.messmate.entity.MealType;
import com.messmate.exception.ApiException;
import com.messmate.repository.FeedbackRepository;
import com.messmate.repository.MealRepository;
import com.messmate.repository.MenuItemRepository;
import com.messmate.repository.ResidentRepository;
import com.messmate.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Read-only reports for the mess committee. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final FeedbackRepository feedbacks;
    private final MealRepository meals;
    private final ResidentRepository residents;
    private final MenuItemRepository menuItems;

    /** Average rating per meal and per day for one week. Meals nobody rated are simply absent. */
    public WeeklyAverages averages(LocalDate weekStart) {
        LocalDate start = Dates.weekStart(weekStart);
        LocalDate end = start.plusDays(6);
        return new WeeklyAverages(start, end, perMeal(start, end), perDay(start, end));
    }

    /** Meals whose average rating is below the threshold, worst first. */
    public List<MealAvg> lowRated(LocalDate weekStart, double threshold) {
        if (threshold < 1 || threshold > 5) {
            throw ApiException.badRequest("threshold must be between 1 and 5.");
        }
        LocalDate start = Dates.weekStart(weekStart);
        return perMeal(start, start.plusDays(6)).stream()
                .filter(m -> m.average() < threshold)
                .sorted(Comparator.comparingDouble(MealAvg::average))
                .toList();
    }

    /** Plates to cook per meal on a date: all residents minus those who opted out. */
    public List<HeadcountRow> headcount(LocalDate date) {
        long total = residents.count();
        return meals.findByMealDate(date).stream()
                .sorted(Comparator.comparing(Meal::getMealType))
                .map(m -> {
                    long out = m.getSkippedBy().size();
                    return new HeadcountRow(m.getId(), m.getMealType(), total, out, total - out);
                })
                .toList();
    }

    public Dashboard dashboard() {
        Double avg = feedbacks.overallAverage();
        return new Dashboard(residents.count(), meals.count(), menuItems.count(), feedbacks.count(),
                avg == null ? null : round(avg));
    }

    private List<MealAvg> perMeal(LocalDate start, LocalDate end) {
        return feedbacks.averagePerMeal(start, end).stream()
                .map(r -> new MealAvg((Long) r[0], (LocalDate) r[1], (MealType) r[2],
                        round(((Number) r[3]).doubleValue()), ((Number) r[4]).longValue()))
                .sorted(Comparator.comparing(MealAvg::date).thenComparing(MealAvg::mealType))
                .toList();
    }

    private List<DayAvg> perDay(LocalDate start, LocalDate end) {
        return feedbacks.averagePerDay(start, end).stream()
                .map(r -> new DayAvg((LocalDate) r[0], round(((Number) r[1]).doubleValue()), ((Number) r[2]).longValue()))
                .toList();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
