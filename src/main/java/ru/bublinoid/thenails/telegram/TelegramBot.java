package ru.bublinoid.thenails.telegram;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bublinoid.thenails.config.BotConfig;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bublinoid.thenails.content.AboutUsInfoProvider;
import ru.bublinoid.thenails.content.BookingInfoProvider;
import ru.bublinoid.thenails.content.ServicesInfoProvider;
import ru.bublinoid.thenails.content.ContactsInfoProvider;
import ru.bublinoid.thenails.keyboard.InlineKeyboardMarkupBuilder;
import ru.bublinoid.thenails.service.BookingService;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig botConfig;
    private final InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder;
    private final ServicesInfoProvider servicesInfoProvider;
    private final AboutUsInfoProvider aboutUsInfoProvider;
    private final ContactsInfoProvider contactsInfoProvider;
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
                sendServicesInfo(chatId, firstName);
                break;
            case "book":
                requestEmailInput(chatId, firstName);
                break;
            case "about_us":
                sendAboutUsInfo(chatId, firstName);
                break;
            case "contacts":
                sendContactsInfo(chatId, firstName);
                break;
            default:
                logger.warn("Unknown callback data: {}", callbackData);
                break;
        }
    }

    private void processEmailInput(long chatId, String email) {
        awaitingEmailInput.put(chatId, false);
        bookingService.handleEmailInput(chatId, email);
        awaitingConfirmationCodeInput.put(chatId, true); // Устанавливаем флаг ожидания ввода кода подтверждения
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
                sendMainMenu(chatId, firstName);
                break;
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Здравствуйте, " + name + "!\n" +
                "Добро пожаловать в наш бот записи на маникюр! Здесь вы сможете легко и быстро записаться на маникюр.";
        logger.info("Sending start command response to chatId: {}, name: {}", chatId, name);
        sendMessageWithKeyboard(chatId, answer, inlineKeyboardMarkupBuilder.createMainMenuKeyboard());
    }

    private void sendServicesInfo(Long chatId, String name) {
        String servicesInfo = servicesInfoProvider.getServicesInfo();
        logger.info("Sending services info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, servicesInfo);
        sendMainMenu(chatId, name);
    }

    private void sendAboutUsInfo(Long chatId, String name) {
        String aboutUsInfo = aboutUsInfoProvider.getAboutUsInfo();
        logger.info("Sending about us info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, aboutUsInfo);
        sendMainMenu(chatId, name);
    }

    private void sendContactsInfo(Long chatId, String name) {
        String contactsInfo = contactsInfoProvider.getContactsInfo();
        logger.info("Sending contacts info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, contactsInfo);
        sendMainMenu(chatId, name);
    }

    private void sendMarkdownMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setParseMode("Markdown"); // Устанавливаем режим парсинга Markdown
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending message: ", e);
        }
    }

    private void requestEmailInput(Long chatId, String name) {
        String bookingInfo = bookingInfoProvider.getRequestEmailMessage();
        logger.info("Requesting email input from chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, bookingInfo);
        awaitingEmailInput.put(chatId, true); // Устанавливаем флаг ожидания ввода e-mail
    }

    private void sendMessageWithKeyboard(Long chatId, String textToSend, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setReplyMarkup(keyboardMarkup);
        sendMessage.setParseMode("Markdown"); // Устанавливаем режим парсинга Markdown для сообщений с клавиатурой
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending message: ", e);
        }
    }

    private void sendMainMenu(Long chatId, String name) {
        logger.info("Sending main menu to chatId: {}, name: {}", chatId, name);
        sendMessageWithKeyboard(chatId, "Что бы вы хотели сделать дальше?", inlineKeyboardMarkupBuilder.createMainMenuKeyboard());
    }

    public void sendInvalidEmailMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidEmailMessage();
        logger.info("Sending invalid email message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailSavedMessage(long chatId) {
        String message = bookingInfoProvider.getEmailSavedMessage();
        logger.info("Sending email saved message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailConfirmedMessage(long chatId) {
        String message = bookingInfoProvider.getEmailConfirmedMessage();
        logger.info("Sending email confirmed message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendInvalidConfirmationCodeMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidCodeMessage();
        logger.info("Sending invalid confirmation code message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendInvalidConfirmationCodeFormatMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidConfirmationCodeFormatMessage();
        logger.info("Sending invalid confirmation code format message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailAlreadyConfirmedMessage(long chatId) {
        String message = "Ваш e-mail уже подтвержден.";
        logger.info("Sending email already confirmed message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }
}
