package ru.bublinoid.thenails.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.service.BookingService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class InlineKeyboardMarkupBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);


    public InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardButton servicesButton = new InlineKeyboardButton();
        servicesButton.setText("Услуги");
        servicesButton.setCallbackData("services");

        InlineKeyboardButton bookButton = new InlineKeyboardButton();
        bookButton.setText("Записаться");
        bookButton.setCallbackData("book");

        InlineKeyboardButton aboutUsButton = new InlineKeyboardButton();
        aboutUsButton.setText("О нас");
        aboutUsButton.setCallbackData("about_us");

        InlineKeyboardButton contactsButton = new InlineKeyboardButton();
        contactsButton.setText("Контакты");
        contactsButton.setCallbackData("contacts");

        InlineKeyboardButton myBookingsButton = new InlineKeyboardButton();
        myBookingsButton.setText("Мои записи");
        myBookingsButton.setCallbackData("my_bookings");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(servicesButton);
        row1.add(bookButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(aboutUsButton);
        row2.add(contactsButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(myBookingsButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createServiceOptionsKeyboard() {
        InlineKeyboardButton manicureButton = new InlineKeyboardButton();
        manicureButton.setText("Маникюр");
        manicureButton.setCallbackData("manicure");

        InlineKeyboardButton fileManicureButton = new InlineKeyboardButton();
        fileManicureButton.setText("Пилочный маникюр");
        fileManicureButton.setCallbackData("file_manicure");

        InlineKeyboardButton complexButton = new InlineKeyboardButton();
        complexButton.setText("Комплекс");
        complexButton.setCallbackData("complex");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(manicureButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(fileManicureButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(complexButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    public InlineKeyboardMarkup createDateSelectionKeyboard(Set<LocalDate> occupiedDates) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        LocalDate startDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalTime cutoffTime = LocalTime.of(18, 0);

        for (int i = 0; i < 15; i++) {
            LocalDate date = startDate.plusDays(i);

            // Check if the date is today and if the current time is after 18:00
            if (date.equals(startDate) && currentTime.isAfter(cutoffTime)) {
                logger.info("Skipping today's date {} because the current time is after 18:00", date);
                continue;
            }

            if (date.getDayOfWeek().getValue() >= 1 && date.getDayOfWeek().getValue() <= 5) {
                if (!occupiedDates.contains(date)) {
                    InlineKeyboardButton dateButton = new InlineKeyboardButton();
                    dateButton.setText(date.format(DATE_FORMATTER));
                    dateButton.setCallbackData("date_" + date);

                    if (rows.isEmpty() || rows.get(rows.size() - 1).size() == 3) {
                        rows.add(new ArrayList<>());
                    }

                    rows.get(rows.size() - 1).add(dateButton);
                }
            }
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }


    public InlineKeyboardMarkup createTimeSelectionKeyboard(LocalDate selectedDate, Set<LocalTime> occupiedTimes) {
        logger.info("Occupied times for date {}: {}", selectedDate, occupiedTimes);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        // Если выбранная дата - сегодня, начинаем с текущего времени плюс один час
        if (selectedDate.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
            startTime = now.isAfter(startTime) ? now : startTime;
        }

        // Генерируем кнопки для доступных временных слотов
        List<InlineKeyboardButton> row = new ArrayList<>();
        while (startTime.isBefore(endTime) || startTime.equals(endTime)) {
            logger.info("Checking time slot: {}", startTime);
            if (!occupiedTimes.contains(startTime)) {
                InlineKeyboardButton timeButton = new InlineKeyboardButton();
                timeButton.setText(startTime.toString());
                timeButton.setCallbackData("time_" + startTime.toString());
                row.add(timeButton);
            } else {
                logger.info("Time slot {} is occupied", startTime);
            }

            // Добавляем ряд кнопок, если он заполнен (максимум 3 кнопки в ряду)
            if (row.size() == 3) {
                rows.add(new ArrayList<>(row));
                row.clear();
            }

            startTime = startTime.plusHours(1);
        }

        // Добавляем оставшиеся кнопки (если есть)
        if (!row.isEmpty()) {
            rows.add(row);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);
        logger.info("Generated time selection keyboard: {}", rows);
        return keyboardMarkup;
    }


    public InlineKeyboardMarkup createConfirmationKeyboard() {
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("Подтвердить запись");
        confirmButton.setCallbackData("confirm_booking"); // Callback data для обработки нажатия кнопки

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(confirmButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createBookingOptionsKeyboard() {
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить запись");
        deleteButton.setCallbackData("delete_booking");

        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton();
        mainMenuButton.setText("Главное меню");
        mainMenuButton.setCallbackData("main_menu");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(deleteButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(mainMenuButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createBookingsKeyboard(List<Booking> bookings) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Booking booking : bookings) {
            InlineKeyboardButton bookingButton = new InlineKeyboardButton();
            bookingButton.setText(booking.getService() + " - " + booking.getDate() + " " + booking.getTime());
            bookingButton.setCallbackData("delete_" + booking.getHash()); // Используем уникальный ID записи

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(bookingButton);
            rows.add(row);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }
}



