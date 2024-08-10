package ru.bublinoid.thenails.telegram;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.bublinoid.thenails.config.BotConfig;
import ru.bublinoid.thenails.content.BookingInfoProvider;
import ru.bublinoid.thenails.service.BookingService;
import ru.bublinoid.thenails.service.MessageService;

import java.util.HashMap;
import java.util.Map;

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
                messageService.sendMarkdownMessage(chatId, "Вы выбрали услугу 'Маникюр'");
                break;
            case "file_manicure":
                messageService.sendMarkdownMessage(chatId, "Вы выбрали услугу 'Пилочный маникюр'");
                break;
            case "complex":
                messageService.sendMarkdownMessage(chatId, "Вы выбрали услугу 'Комплекс'");
                break;
            default:
                logger.warn("Unknown callback data: {}", callbackData);
                break;
        }
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
