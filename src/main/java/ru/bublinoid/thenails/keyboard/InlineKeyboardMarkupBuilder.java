package ru.bublinoid.thenails.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
@Component
public class InlineKeyboardMarkupBuilder {

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

    public InlineKeyboardMarkup createDateSelectionKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 0; i < 15; i++) {
            LocalDate date = startDate.plusDays(i);
            if (date.getDayOfWeek().getValue() >= 1 && date.getDayOfWeek().getValue() <= 5) {
                InlineKeyboardButton dateButton = new InlineKeyboardButton();
                dateButton.setText(date.format(DATE_FORMATTER));
                dateButton.setCallbackData("date_" + date);

                if (rows.isEmpty() || rows.get(rows.size() - 1).size() == 3) {
                    rows.add(new ArrayList<>());
                }

                rows.get(rows.size() - 1).add(dateButton);
            }
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createTimeSelectionKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        String[] times = {"10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"};

        for (int i = 0; i < times.length; i += 3) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = 0; j < 3 && i + j < times.length; j++) {
                InlineKeyboardButton timeButton = new InlineKeyboardButton();
                timeButton.setText(times[i + j]);
                timeButton.setCallbackData("time_" + times[i + j]);
                row.add(timeButton);
            }
            rows.add(row);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);
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

    public InlineKeyboardMarkup createBookingsMenuKeyboard() {
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить запись");
        deleteButton.setCallbackData("delete_");  // Пример данных callback, их следует подставить после получения информации о записи

        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton();
        mainMenuButton.setText("Главное меню");
        mainMenuButton.setCallbackData("main_menu");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(deleteButton);
        row1.add(mainMenuButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createDeleteConfirmationKeyboard(String service, String date, String time) {
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("Подтвердить удаление");
        confirmButton.setCallbackData("confirm_delete_" + service + "_" + date + "_" + time);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отмена");
        cancelButton.setCallbackData("my_bookings");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(confirmButton);
        row.add(cancelButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }
}

