package ru.bublinoid.thenails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bublinoid.thenails.content.*;
import ru.bublinoid.thenails.keyboard.InlineKeyboardMarkupBuilder;
import ru.bublinoid.thenails.model.Booking;
import ru.bublinoid.thenails.telegram.TelegramBot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Service class responsible for handling messages sent to users via Telegram.
 * This includes sending plain text messages, messages with inline keyboards, and various informational messages.
 */

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private TelegramBot telegramBot;

    private final InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder;
    private final ServicesInfoProvider servicesInfoProvider;
    private final AboutUsInfoProvider aboutUsInfoProvider;
    private final ContactsInfoProvider contactsInfoProvider;
    private final BookingInfoProvider bookingInfoProvider;

    private final DiscountInfoProvider discountInfoProvider;

    @Autowired
    public MessageService(InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder,
                          ServicesInfoProvider servicesInfoProvider,
                          AboutUsInfoProvider aboutUsInfoProvider,
                          ContactsInfoProvider contactsInfoProvider,
                          BookingInfoProvider bookingInfoProvider, DiscountInfoProvider discountInfoProvider) {
        this.inlineKeyboardMarkupBuilder = inlineKeyboardMarkupBuilder;
        this.servicesInfoProvider = servicesInfoProvider;
        this.aboutUsInfoProvider = aboutUsInfoProvider;
        this.contactsInfoProvider = contactsInfoProvider;
        this.bookingInfoProvider = bookingInfoProvider;
        this.discountInfoProvider = discountInfoProvider;
    }

    /**
     * Sets the Telegram bot instance.
     * This is done lazily to avoid circular dependencies.
     *
     * @param telegramBot the Telegram bot instance.
     */
    @Lazy
    @Autowired
    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public InlineKeyboardMarkup createMainMenuKeyboard() {
        return inlineKeyboardMarkupBuilder.createMainMenuKeyboard();
    }

    public InlineKeyboardMarkup createBookingOptionsKeyboard() {
        return inlineKeyboardMarkupBuilder.createBookingOptionsKeyboard();
    }

    public InlineKeyboardMarkup createBookingsKeyboard(List<Booking> bookings) {
        return inlineKeyboardMarkupBuilder.createBookingsKeyboard(bookings);
    }


    /**
     * Sends a markdown-formatted message to the user.
     *
     * @param chatId     the chat ID of the user.
     * @param textToSend the text to send.
     */
    public void sendMarkdownMessage(Long chatId, String textToSend) {
        logger.debug("Sending message to chatId: {}, text: {}", chatId, textToSend);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setParseMode("Markdown");
        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending message: ", e);
        }
    }

    public void sendMessageWithKeyboard(Long chatId, String textToSend, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setReplyMarkup(keyboardMarkup);
        sendMessage.setParseMode("Markdown");

        try {
            telegramBot.execute(sendMessage);
            logger.debug("Message with keyboard sent successfully to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending message with keyboard to chatId: {}", chatId, e);
        }
    }

    public void sendServicesInfo(Long chatId, String name) {
        String servicesInfo = servicesInfoProvider.getServicesInfo();
        logger.debug("Sending services info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, servicesInfo);
        sendMainMenu(chatId, name);
    }

    public void sendAboutUsInfo(Long chatId, String name) {
        String aboutUsInfo = aboutUsInfoProvider.getAboutUsInfo();
        logger.debug("Sending about us info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, aboutUsInfo);
        sendMainMenu(chatId, name);
    }

    public void sendContactsInfo(Long chatId, String name) {
        String contactsInfo = contactsInfoProvider.getContactsInfo();
        logger.debug("Sending contacts info to chatId: {}, name: {}", chatId, name);
        sendMarkdownMessage(chatId, contactsInfo);
        sendMainMenu(chatId, name);
    }

    public void sendMainMenu(Long chatId, String name) {
        logger.debug("Sending main menu to chatId: {}, name: {}", chatId, name);
        sendMessageWithKeyboard(chatId, "Что бы вы хотели сделать дальше?", inlineKeyboardMarkupBuilder.createMainMenuKeyboard());
    }

    public void sendInvalidEmailMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidEmailMessage();
        logger.warn("Sending invalid email message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailSavedMessage(long chatId) {
        String message = bookingInfoProvider.getEmailSavedMessage();
        logger.debug("Sending email saved message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailConfirmedMessage(long chatId) {
        String message = bookingInfoProvider.getEmailConfirmedMessage();
        logger.debug("Sending email confirmed message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);

        logger.debug("Attempting to send service options to chatId: {}", chatId);
        sendServiceOptions(chatId);
    }

    public void sendServiceOptions(long chatId) {
        InlineKeyboardMarkup serviceOptionsKeyboard = inlineKeyboardMarkupBuilder.createServiceOptionsKeyboard();
        if (serviceOptionsKeyboard != null && !serviceOptionsKeyboard.getKeyboard().isEmpty()) {
            sendMessageWithKeyboard(chatId, "Пожалуйста, выберите одну из услуг:", serviceOptionsKeyboard);
            logger.debug("Service options sent successfully to chatId: {}", chatId);
        } else {
            logger.warn("Failed to create service options keyboard or keyboard is empty for chatId: {}", chatId);
        }
    }

    public void sendInvalidConfirmationCodeMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidCodeMessage();
        logger.warn("Sending invalid confirmation code message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendInvalidConfirmationCodeFormatMessage(long chatId) {
        String message = bookingInfoProvider.getInvalidConfirmationCodeFormatMessage();
        logger.warn("Sending invalid confirmation code format message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
    }

    public void sendEmailAlreadyConfirmedMessage(long chatId) {
        String message = "Ваш e-mail уже подтвержден.";
        logger.debug("Sending email already confirmed message to chatId: {}", chatId);
        sendMarkdownMessage(chatId, message);
        sendServiceOptions(chatId);
    }

    public void sendDateSelection(Long chatId, Set<LocalDate> occupiedDates) {
        InlineKeyboardMarkup dateKeyboard = inlineKeyboardMarkupBuilder.createDateSelectionKeyboard(occupiedDates);
        sendMessageWithKeyboard(chatId, "Выберите удобную дату для услуги:", dateKeyboard);
    }

    public void sendTimeSelection(Long chatId, String selectedDate, Set<LocalTime> occupiedTimes) {
        InlineKeyboardMarkup timeKeyboard = inlineKeyboardMarkupBuilder.createTimeSelectionKeyboard(LocalDate.parse(selectedDate), occupiedTimes);
        sendMessageWithKeyboard(chatId, "Выберите удобное время для даты: " + selectedDate, timeKeyboard);
    }

    public void sendConfirmationRequest(Long chatId, String service, LocalDate date, LocalTime time) {
        String confirmationMessage = "Вы выбрали услугу: " + service + " на дату: " + date + " в " + time;
        InlineKeyboardMarkup confirmationKeyboard = inlineKeyboardMarkupBuilder.createConfirmationKeyboard();
        sendMessageWithKeyboard(chatId, confirmationMessage, confirmationKeyboard);
        logger.debug("Sent confirmation request for service: {}, date: {}, time: {} to chatId: {}", service, date, time, chatId);
    }

    public void sendDiscountInfo(Long chatId) {
        String discountInfo = discountInfoProvider.getDiscountInfo();
        logger.debug("Sending discount info to chatId: {}", chatId);
        InlineKeyboardMarkup playButtonKeyboard = inlineKeyboardMarkupBuilder.createPlayButtonKeyboard();
        sendMessageWithKeyboard(chatId, discountInfo, playButtonKeyboard);
    }

}
