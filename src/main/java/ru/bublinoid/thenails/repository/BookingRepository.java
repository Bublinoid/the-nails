package ru.bublinoid.thenails.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.bublinoid.thenails.model.Booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByChatIdAndServiceAndDateAndTime(Long chatId, String service, LocalDate date, LocalTime time);

    List<Booking> findByChatIdAndConfirmTrue(Long chatId);

    Optional<Booking> findByHash(UUID hash);

    List<Booking> findByDateAndConfirmTrue(LocalDate date);

    @Query("SELECT b.date FROM Booking b WHERE b.confirm = true")
    Set<LocalDate> findOccupiedDates();

    @Query("SELECT b.time FROM Booking b WHERE b.date = :date AND b.confirm = true")
    Set<LocalTime> findOccupiedTimesByDate(LocalDate date);
}
