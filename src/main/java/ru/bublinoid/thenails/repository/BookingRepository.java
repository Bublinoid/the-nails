package ru.bublinoid.thenails.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bublinoid.thenails.model.Booking;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Дополнительные методы при необходимости
}
