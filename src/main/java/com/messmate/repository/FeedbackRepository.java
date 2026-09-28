package com.messmate.repository;

import com.messmate.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByResidentIdAndMealId(Long residentId, Long mealId);

    List<Feedback> findByMealId(Long mealId);

    List<Feedback> findByResidentId(Long residentId);

    /** Columns: meal id, meal date, meal type, average rating, number of ratings. */
    @Query("select m.id, m.mealDate, m.mealType, avg(f.rating), count(f) " +
           "from Feedback f join f.meal m " +
           "where m.mealDate between :start and :end " +
           "group by m.id, m.mealDate, m.mealType")
    List<Object[]> averagePerMeal(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Columns: date, average of all ratings given for meals that day, number of ratings. */
    @Query("select m.mealDate, avg(f.rating), count(f) " +
           "from Feedback f join f.meal m " +
           "where m.mealDate between :start and :end " +
           "group by m.mealDate order by m.mealDate")
    List<Object[]> averagePerDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select avg(f.rating) from Feedback f")
    Double overallAverage();
}
