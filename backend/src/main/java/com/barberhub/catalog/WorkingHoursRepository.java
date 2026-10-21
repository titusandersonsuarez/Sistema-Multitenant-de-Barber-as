package com.barberhub.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findByBarberIdOrderByDayOfWeekAscStartTimeAsc(Long barberId);

    List<WorkingHours> findByBarberIdAndDayOfWeekOrderByStartTimeAsc(Long barberId, short dayOfWeek);

    @Modifying
    @Query("delete from WorkingHours w where w.barberId = :barberId")
    void deleteByBarberId(@Param("barberId") Long barberId);
}
