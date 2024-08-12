package ru.bublinoid.thenails.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.repository.BookingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    private final EmailService emailService;

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
