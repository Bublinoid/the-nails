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

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(servicesButton);
        row1.add(bookButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(aboutUsButton);
        row2.add(contactsButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

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
}

