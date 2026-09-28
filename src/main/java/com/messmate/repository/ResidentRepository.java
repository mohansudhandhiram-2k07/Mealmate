package com.messmate.repository;

import com.messmate.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select count(m) from Resident r join r.skippedMeals m where r.id = :residentId and m.id = :mealId")
    long countOptOut(@Param("residentId") Long residentId, @Param("mealId") Long mealId);

    @Query("select m.id from Resident r join r.skippedMeals m where r.id = :residentId")
    List<Long> findSkippedMealIds(@Param("residentId") Long residentId);
}
