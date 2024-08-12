package ru.bublinoid.thenails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.repository.BookingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    public BookingService(BookingRepository bookingRepository, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
    }

    public void handleEmailInput(long chatId, String email) {
        emailService.handleEmailInput(chatId, email);
    }

    public void confirmEmailCode(long chatId, String code) {
        emailService.confirmEmailCode(chatId, code);
    }

    public void saveBooking(Long chatId, String service, LocalDate date, LocalTime time) {
        // Получаем существующий хеш из таблицы email по chatId
        UUID existingHash = emailService.getHashByChatId(chatId);

        if (existingHash == null) {
            // Обработка ошибки, если хеш не найден
            throw new IllegalArgumentException("Hash not found for chatId: " + chatId);
        }

        Booking booking = new Booking();
        booking.setHash(existingHash); // Используем существующий хеш
        booking.setChatId(chatId);
        booking.setService(service);
        booking.setDate(date);
        booking.setTime(time);
        booking.setConfirm(false); // Изначально запись не подтверждена

        bookingRepository.save(booking);
    }

    public void confirmBooking(Long chatId, String service, LocalDate date, LocalTime time) {
        Optional<Booking> bookingOptional = bookingRepository.findByChatIdAndServiceAndDateAndTime(chatId, service, date, time);

        if (bookingOptional.isPresent()) {
            Booking booking = bookingOptional.get();
            booking.setConfirm(true);
            bookingRepository.save(booking);
        } else {
            throw new IllegalArgumentException("Booking not found for chatId: " + chatId + ", service: " + service + ", date: " + date + ", time: " + time);
        }
    }

    public Set<LocalDate> getOccupiedDates() {
        logger.info("Retrieving occupied dates");

        // Получаем все записи с подтвержденными бронированиями
        List<Booking> confirmedBookings = bookingRepository.findAll().stream()
                .filter(Booking::getConfirm)
                .toList();

        // Выбираем только те даты, для которых уже все время занято
        Set<LocalDate> occupiedDates = confirmedBookings.stream()
                .collect(Collectors.groupingBy(Booking::getDate, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= 9) // предполагается, что все слоты заняты, если 9 или более записей на дату
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        logger.info("Occupied dates: {}", occupiedDates);
        return occupiedDates;
    }

    public Set<LocalTime> getOccupiedTimesForDate(LocalDate date) {
        logger.info("Retrieving occupied times for date: {}", date);

        // Получаем все подтвержденные бронирования на указанную дату
        List<Booking> bookings = bookingRepository.findByDateAndConfirmTrue(date);

        // Извлекаем занятые временные слоты
        Set<LocalTime> occupiedTimes = bookings.stream()
                .map(Booking::getTime)
                .collect(Collectors.toSet());

        logger.info("Occupied times for date {}: {}", date, occupiedTimes);
        return occupiedTimes;
    }

    public String getMyBookingsInfo(Long chatId) {
        List<Booking> bookings = bookingRepository.findByChatIdAndConfirmTrue(chatId);
        if (bookings.isEmpty()) {
            return "У вас нет записей.";
        } else {
            StringBuilder sb = new StringBuilder("Ваши записи:\n");
            for (Booking booking : bookings) {
                sb.append("Услуга: ").append(booking.getService())
                        .append(", Дата: ").append(booking.getDate())
                        .append(", Время: ").append(booking.getTime())
                        .append("\n");
            }
            return sb.toString();
        }
    }

    public void deleteBookingByHash(UUID hash) {
        Optional<Booking> bookingOptional = bookingRepository.findByHash(hash);

        if (bookingOptional.isPresent()) {
            bookingRepository.delete(bookingOptional.get());
        } else {
            throw new IllegalArgumentException("Запись не найдена для hash: " + hash);
        }
    }

    public List<Booking> getBookingsByChatId(long chatId) {
        return bookingRepository.findByChatIdAndConfirmTrue(chatId);
    }

}
