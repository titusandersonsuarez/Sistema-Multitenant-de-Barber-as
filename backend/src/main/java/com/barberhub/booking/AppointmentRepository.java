package com.barberhub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByCode(String code);

    /** Citas que ocupan agenda (todo menos canceladas) y se cruzan con [from, to). */
    @Query("""
            select a from Appointment a
            where a.barberId = :barberId
              and a.status <> com.barberhub.booking.AppointmentStatus.CANCELLED
              and a.startAt < :to and a.endAt > :from
            order by a.startAt
            """)
    List<Appointment> findBlocking(@Param("barberId") Long barberId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("""
            select a from Appointment a
            where a.startAt >= :from and a.startAt < :to
              and (:barberId is null or a.barberId = :barberId)
            order by a.startAt
            """)
    List<Appointment> findAgenda(@Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("barberId") Long barberId);

    @Query("""
            select count(a) from Appointment a
            where a.customerPhone = :phone
              and a.status = com.barberhub.booking.AppointmentStatus.BOOKED
              and a.startAt > :now
            """)
    long countActiveByPhone(@Param("phone") String phone, @Param("now") LocalDateTime now);

    @Query("""
            select a from Appointment a
            where a.startAt >= :from and a.startAt < :to
            """)
    List<Appointment> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
