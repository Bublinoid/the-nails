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
            return "У вас нет подтвержденных записей.";
        } else {
            StringBuilder sb = new StringBuilder("Ваши подтвержденные записи:\n");
            for (Booking booking : bookings) {
                String bookingIdentifier = String.format("%s_%s_%s", booking.getService(), booking.getDate(), booking.getTime());
                sb.append("Услуга: ").append(booking.getService())
                        .append(", Дата: ").append(booking.getDate())
                        .append(", Время: ").append(booking.getTime())
                        .append("\n");

                sb.append("Удалить запись: /delete_").append(bookingIdentifier).append("\n");
            }
            return sb.toString();
        }
    }

    public void deleteBookingByIdentifier(Long chatId, String service, String date, String time) {
        Optional<Booking> bookingOptional = bookingRepository.findByChatIdAndServiceAndDateAndTime(chatId, service, LocalDate.parse(date), LocalTime.parse(time));
        if (bookingOptional.isPresent()) {
            bookingRepository.delete(bookingOptional.get());
        } else {
            throw new IllegalArgumentException("Запись не найдена для chatId: " + chatId + ", услуга: " + service + ", дата: " + date + ", время: " + time);
        }
    }
}
