package ru.bublinoid.thenails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.repository.BookingRepository;

import java.time.LocalDate;;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Service class for managing bookings.
 */


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

    /**
     * Saves a new booking
     *
     * @param chatId  the chat ID of the user.
     * @param service the service booked.
     * @param date    the date of the booking.
     * @param time    the time of the booking.
     */
    public void saveBooking(Long chatId, String service, LocalDate date, LocalTime time) {
        Booking booking = new Booking();
        booking.setHash(UUID.randomUUID()); // Generate a new hash for each booking
        booking.setChatId(chatId);
        booking.setService(service);
        booking.setDate(date);
        booking.setTime(time);
        booking.setConfirm(false); // Initially, the booking is not confirmed

        bookingRepository.save(booking);
        logger.debug("Booking saved for chatId: {}, service: {}, date: {}, time: {}", chatId, service, date, time);
    }

    public void confirmBooking(Long chatId, String service, LocalDate date, LocalTime time) {
        Optional<Booking> bookingOptional = bookingRepository.findByChatIdAndServiceAndDateAndTime(chatId, service, date, time);

        if (bookingOptional.isPresent()) {
            Booking booking = bookingOptional.get();
            booking.setConfirm(true);
            bookingRepository.save(booking);
            logger.debug("Booking confirmed for chatId: {}, service: {}, date: {}, time: {}", chatId, service, date, time);
        } else {
            throw new IllegalArgumentException("Booking not found for chatId: " + chatId + ", service: " + service + ", date: " + date + ", time: " + time);
        }
    }

    /**
     * Retrieves the set of dates that are fully booked.
     *
     * @return a set of occupied dates.
     */
    public Set<LocalDate> getOccupiedDates() {
        logger.debug("Retrieving occupied dates");

        // Get all bookings that are confirmed
        List<Booking> confirmedBookings = bookingRepository.findAll().stream()
                .filter(Booking::getConfirm)
                .toList();

        // Select only those dates where all time slots are occupied
        Set<LocalDate> occupiedDates = confirmedBookings.stream()
                .collect(Collectors.groupingBy(Booking::getDate, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= 9) // Assume all slots are occupied if there are 9 or more bookings on a date
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        logger.debug("Occupied dates: {}", occupiedDates);
        return occupiedDates;
    }

    /**
     * Retrieves the set of times that are fully booked on a specific date.
     *
     * @param date the date to check.
     * @return a set of occupied times on the given date.
     */
    public Set<LocalTime> getOccupiedTimesForDate(LocalDate date) {
        logger.debug("Retrieving occupied times for date: {}", date);

        // Get all confirmed bookings for the specified date
        List<Booking> bookings = bookingRepository.findByDateAndConfirmTrue(date);

        // Extract the occupied time slots
        Set<LocalTime> occupiedTimes = bookings.stream()
                .map(Booking::getTime)
                .collect(Collectors.toSet());

        logger.debug("Occupied times for date {}: {}", date, occupiedTimes);
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
            logger.debug("Bookings found for chatId: {}", chatId);
            return sb.toString();
        }
    }

    public void deleteBookingByHash(UUID hash) {
        Optional<Booking> bookingOptional = bookingRepository.findByHash(hash);

        if (bookingOptional.isPresent()) {
            bookingRepository.delete(bookingOptional.get());
            logger.debug("Booking deleted for hash: {}", hash);
        } else {
            throw new IllegalArgumentException("Booking not found for hash: " + hash);
        }
    }

    public List<Booking> getBookingsByChatId(long chatId) {
        return bookingRepository.findByChatIdAndConfirmTrue(chatId);
    }

}
