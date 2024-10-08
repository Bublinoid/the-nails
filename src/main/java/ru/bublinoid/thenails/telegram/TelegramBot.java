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
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.service.BookingService;
import ru.bublinoid.thenails.service.DiscountService;
import ru.bublinoid.thenails.service.MessageService;
import ru.bublinoid.thenails.utils.EmailValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig botConfig;
    private final MessageService messageService;
    private final BookingService bookingService;
    private final DiscountService discountService;
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

    /**
     * Handles incoming updates (messages or callback queries) from users.
     *
     * @param update the update received from Telegram API
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    /**
     * Handles text messages received from users.
     *
     * @param update the update containing the text message
     */
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
        //TODO: сделать нормальную обработку команд через функ. интерфейс + создать карту команд для обработки
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String firstName = update.getCallbackQuery().getMessage().getChat().getFirstName();

        logger.info("Received callback query from chatId: {}, name: {}, data: {}", chatId, firstName, callbackData);

        if (callbackData.startsWith("date_")) {
            String selectedDate = callbackData.substring(5);
            selectedDates.put(chatId, selectedDate);

            Set<LocalTime> occupiedTimes = bookingService.getOccupiedTimesForDate(LocalDate.parse(selectedDate));
            messageService.sendTimeSelection(chatId, selectedDate, occupiedTimes);

        } else if (callbackData.startsWith("time_")) {
            String selectedTime = callbackData.substring(5);
            selectedTimes.put(chatId, selectedTime);
            confirmBooking(chatId);
        } else if ("confirm_booking".equals(callbackData)) {
            String service = TelegramBot.getServiceNames().getOrDefault(selectedServices.get(chatId), "Услуга не выбрана");
            LocalDate date = LocalDate.parse(selectedDates.get(chatId));
            LocalTime time = LocalTime.parse(selectedTimes.get(chatId));

            // Save the booking in the database
            bookingService.saveBooking(chatId, service, date, time);
            bookingService.confirmBooking(chatId, service, date, time);
            messageService.sendMarkdownMessage(chatId, "Ваша запись подтверждена!");
            messageService.sendMainMenu(chatId, firstName);

        } else if ("discount".equals(callbackData)) {
            messageService.sendDiscountInfo(chatId);

        } else if ("play_discount_game".equals(callbackData)) {
            discountService.playDiscountGame(chatId);
            messageService.sendMainMenu(chatId, firstName);


        } else if (callbackData.equals("my_bookings")) {
            List<Booking> bookings = bookingService.getBookingsByChatId(chatId);
            if (bookings.isEmpty()) {
                messageService.sendMarkdownMessage(chatId, "У вас нет записей.");
                messageService.sendMainMenu(chatId, firstName);
            } else {
                String myBookingsInfo = bookingService.getMyBookingsInfo(chatId);
                messageService.sendMarkdownMessage(chatId, myBookingsInfo);

                InlineKeyboardMarkup keyboard = messageService.createBookingOptionsKeyboard();
                messageService.sendMessageWithKeyboard(chatId, "Выберите действие:", keyboard);
            }
        } else if (callbackData.equals("delete_booking")) {
            List<Booking> bookings = bookingService.getBookingsByChatId(chatId);
            InlineKeyboardMarkup keyboard = messageService.createBookingsKeyboard(bookings);
            messageService.sendMessageWithKeyboard(chatId, "Выберите запись для удаления:", keyboard);
        } else if (callbackData.startsWith("delete_")) {
            // Handle booking deletion by hash
            try {
                String bookingHash = callbackData.substring(7);
                bookingService.deleteBookingByHash(UUID.fromString(bookingHash));
                messageService.sendMarkdownMessage(chatId, "Запись успешно удалена.");
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID string: {}", e.getMessage());
                messageService.sendMarkdownMessage(chatId, "Произошла ошибка. Некорректный идентификатор записи.");
            }
            messageService.sendMainMenu(chatId, firstName);
        } else if (callbackData.equals("main_menu")) {
            messageService.sendMainMenu(chatId, firstName);

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
                    Set<LocalDate> occupiedDates = bookingService.getOccupiedDates();

                    // Pass the list of occupied dates to sendDateSelection method
                    messageService.sendDateSelection(chatId, occupiedDates);
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
        messageService.sendConfirmationRequest(chatId, service, date, time);
    }


    private void processEmailInput(long chatId, String email) {
        if (EmailValidator.isValid(email)) {
            awaitingEmailInput.put(chatId, false);
            bookingService.handleEmailInput(chatId, email);
            awaitingConfirmationCodeInput.put(chatId, true);
        } else {
            logger.warn("Received invalid email: {} from chatId: {}", email, chatId);
            messageService.sendInvalidEmailMessage(chatId);
            awaitingEmailInput.put(chatId, true);
            awaitingConfirmationCodeInput.put(chatId, false); // Reset confirmation code awaiting flag
        }
    }

    private void processConfirmationCodeInput(long chatId, String code) {
        if (awaitingConfirmationCodeInput.getOrDefault(chatId, false)) {
            bookingService.confirmEmailCode(chatId, code);
            awaitingConfirmationCodeInput.put(chatId, false);
        } else {
            logger.warn("Attempted confirmation code input without prior email input for chatId: {}", chatId);
            messageService.sendInvalidConfirmationCodeFormatMessage(chatId);
        }
    }

    private void processCommand(long chatId, String firstName, String command) {
        if (command.equals("/start")) {
            startCommandReceived(chatId, firstName);
        } else {
            messageService.sendMainMenu(chatId, firstName);
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
        logger.debug("Requesting email input from chatId: {}, name: {}", chatId, name);
        messageService.sendMarkdownMessage(chatId, bookingInfo);
        awaitingEmailInput.put(chatId, true);
    }

    public void setAwaitingConfirmationCodeInput(long chatId, boolean awaiting) {
        awaitingConfirmationCodeInput.put(chatId, awaiting);
    }
}
