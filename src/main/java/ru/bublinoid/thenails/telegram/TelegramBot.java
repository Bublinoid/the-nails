package ru.bublinoid.thenails.telegram;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bublinoid.thenails.config.BotConfig;
import ru.bublinoid.thenails.content.BookingInfoProvider;
import ru.bublinoid.thenails.service.BookingService;
import ru.bublinoid.thenails.service.MessageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig botConfig;
    private final MessageService messageService;
    private final BookingService bookingService;
    private final BookingInfoProvider bookingInfoProvider;
    private final Map<Long, Boolean> awaitingEmailInput = new HashMap<>();
    private final Map<Long, Boolean> awaitingConfirmationCodeInput = new HashMap<>();
    private final Map<Long, String> selectedServices = new HashMap<>();
    private final Map<Long, String> selectedDates = new HashMap<>();
    private final Map<Long, String> selectedTimes = new HashMap<>();
    @Getter
    private static final Map<String, String> serviceNames = new HashMap<>();

    static {
        serviceNames.put("manicure", "Маникюр");
        serviceNames.put("file_manicure", "Пилочный маникюр");
        serviceNames.put("complex", "Комплекс");
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();

        logger.info("Received message from chatId: {}, name: {}, text: {}", chatId, firstName, messageText);

        if (awaitingEmailInput.getOrDefault(chatId, false)) {
            processEmailInput(chatId, messageText);
        } else if (awaitingConfirmationCodeInput.getOrDefault(chatId, false)) {
            processConfirmationCodeInput(chatId, messageText);
        } else {
            processCommand(chatId, firstName, messageText);
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String firstName = update.getCallbackQuery().getMessage().getChat().getFirstName();

        logger.info("Received callback query from chatId: {}, name: {}, data: {}", chatId, firstName, callbackData);

        if (callbackData.startsWith("date_")) {
            String selectedDate = callbackData.substring(5);
            selectedDates.put(chatId, selectedDate);
            messageService.sendTimeSelection(chatId, selectedDate);
        } else if (callbackData.startsWith("time_")) {
            String selectedTime = callbackData.substring(5);
            selectedTimes.put(chatId, selectedTime);
            confirmBooking(chatId);
        } else if ("confirm_booking".equals(callbackData)) {
            String service = TelegramBot.getServiceNames().getOrDefault(selectedServices.get(chatId), "Услуга не выбрана");
            LocalDate date = LocalDate.parse(selectedDates.get(chatId));
            LocalTime time = LocalTime.parse(selectedTimes.get(chatId));

            // Сохраняем бронирование в базе данных
            bookingService.saveBooking(chatId, service, date, time);
            bookingService.confirmBooking(chatId, service, date, time);
            messageService.sendMarkdownMessage(chatId, "Ваша запись подтверждена!");
        } else if ("my_bookings".equals(callbackData)) {
            String myBookingsInfo = bookingService.getMyBookingsInfo(chatId);
            messageService.sendMarkdownMessage(chatId, myBookingsInfo);
        } else if (callbackData.startsWith("delete_")) {
            String[] parts = callbackData.substring(7).split("_");
            String service = parts[0];
            LocalDate date = LocalDate.parse(parts[1]);
            LocalTime time = LocalTime.parse(parts[2]);

            // Удаление записи по идентификатору
            bookingService.deleteBookingByIdentifier(chatId, service, date, time);
            messageService.sendMarkdownMessage(chatId, "Запись успешно удалена.");
        } else {
            switch (callbackData) {
                case "services":
                    messageService.sendServicesInfo(chatId, firstName);
                    break;
                case "book":
                    requestEmailInput(chatId, firstName);
                    break;
                case "about_us":
                    messageService.sendAboutUsInfo(chatId, firstName);
                    break;
                case "contacts":
                    messageService.sendContactsInfo(chatId, firstName);
                    break;
                case "manicure":
                case "file_manicure":
                case "complex":
                    selectedServices.put(chatId, callbackData);
                    messageService.sendDateSelection(chatId, callbackData);
                    break;
                default:
                    logger.warn("Unknown callback data: {}", callbackData);
                    break;
            }
        }
    }

    private void confirmBooking(long chatId) {
        String service = TelegramBot.getServiceNames().getOrDefault(selectedServices.get(chatId), "Услуга не выбрана");
        LocalDate date = LocalDate.parse(selectedDates.get(chatId));
        LocalTime time = LocalTime.parse(selectedTimes.get(chatId));

        // Используем MessageService для отправки подтверждения
        messageService.sendConfirmationRequest(chatId, service, date, time);
    }


    private void processEmailInput(long chatId, String email) {
        awaitingEmailInput.put(chatId, false);
        bookingService.handleEmailInput(chatId, email);
        awaitingConfirmationCodeInput.put(chatId, true);
    }

    private void processConfirmationCodeInput(long chatId, String code) {
        awaitingConfirmationCodeInput.put(chatId, false);
        bookingService.confirmEmailCode(chatId, code);
    }

    private void processCommand(long chatId, String firstName, String command) {
        switch (command) {
            case "/start":
                startCommandReceived(chatId, firstName);
                break;
            default:
                messageService.sendMainMenu(chatId, firstName);
                break;
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Здравствуйте, " + name + "!\n" +
                "Добро пожаловать в наш бот записи на маникюр! Здесь вы сможете легко и быстро записаться на маникюр.";
        logger.info("Sending start command response to chatId: {}, name: {}", chatId, name);
        messageService.sendMessageWithKeyboard(chatId, answer, messageService.createMainMenuKeyboard());
    }

    private void requestEmailInput(Long chatId, String name) {
        String bookingInfo = bookingInfoProvider.getRequestEmailMessage();
        logger.info("Requesting email input from chatId: {}, name: {}", chatId, name);
        messageService.sendMarkdownMessage(chatId, bookingInfo);
        awaitingEmailInput.put(chatId, true);
    }

    public void setAwaitingConfirmationCodeInput(long chatId, boolean awaiting) {
        awaitingConfirmationCodeInput.put(chatId, awaiting);
    }
}
