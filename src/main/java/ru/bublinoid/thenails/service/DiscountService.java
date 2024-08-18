package ru.bublinoid.thenails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDice;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bublinoid.thenails.telegram.TelegramBot;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for managing discount games in the Telegram bot.
 */

@Service
public class DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);

    private final TelegramBot telegramBot;
    private final MessageService messageService;

    // Stores the last game date for each user
    private final Map<Long, LocalDate> lastPlayedDateMap = new HashMap<>();

    @Autowired
    public DiscountService(@Lazy TelegramBot telegramBot, MessageService messageService) {
        this.telegramBot = telegramBot;
        this.messageService = messageService;
    }

    /**
     * Initiates the discount game for the user. Checks if the user has already played today,
     * rolls two dice, and sends the appropriate message based on the result.
     *
     * @param chatId the chat ID of the user.
     */
    public void playDiscountGame(Long chatId) {
        // Check if the user has already played today
        if (hasPlayedToday(chatId)) {
            String message = "Вы уже играли сегодня. Попробуйте снова завтра!";
            messageService.sendMarkdownMessage(chatId, message);
            logger.debug("User with chatId {} already played today.", chatId);
            return;
        }

        // Roll the first dice
        int firstDiceValue = rollDice(chatId);
        if (firstDiceValue == -1) {
            return;
        }

        // Roll the second dice
        // TODO: Реализовать задержку для ожидания завершения анимации первого кубика и второго
        //краткая пауза или асинхронное ожидание
        int secondDiceValue = rollDice(chatId);
        if (secondDiceValue == -1) {
            return;
        }

        // Update the last played date for the user
        lastPlayedDateMap.put(chatId, LocalDate.now());

        // Check if the dice values match
        if (firstDiceValue == secondDiceValue) {
            sendDiscountWinMessage(chatId, firstDiceValue);
        } else {
            sendNoDiscountMessage(chatId, firstDiceValue, secondDiceValue);
        }
    }

    private boolean hasPlayedToday(Long chatId) {
        LocalDate lastPlayedDate = lastPlayedDateMap.get(chatId);
        return lastPlayedDate != null && lastPlayedDate.equals(LocalDate.now());
    }


    /**
     * Sends a dice roll message to the user and returns the value of the dice.
     *
     * @param chatId the chat ID of the user.
     * @return the value of the rolled dice, or -1 if there was an error.
     */
    private int rollDice(Long chatId) {
        SendDice sendDice = new SendDice();
        sendDice.setChatId(chatId);
        sendDice.setEmoji("🎲");  // Rolling dice emoji

        try {
            Message message = telegramBot.execute(sendDice); // Sending dice animation
            return message.getDice().getValue();
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending dice: ", e);
            return -1; // Return -1 in case of an error
        }
    }

    /**
     * Sends a message to the user if they won a discount.
     *
     * @param chatId         the chat ID of the user.
     * @param firstDiceValue the value of the first dice.
     */
    private void sendDiscountWinMessage(Long chatId, int firstDiceValue) {
        String message = "Поздравляем! Вам выпало два кубика с одинаковым значением " + firstDiceValue +
                ". Вы выиграли скидку 15%!";
        messageService.sendMarkdownMessage(chatId, message);
        logger.debug("Sent discount win message to chatId {}: {}", chatId, message);
    }

    private void sendNoDiscountMessage(Long chatId, int firstDiceValue, int secondDiceValue) {
        String message = "Вам выпало " + firstDiceValue + " и " + secondDiceValue +
                ". К сожалению, скидку выиграть не удалось. Попробуйте через 24 часа!";
        messageService.sendMarkdownMessage(chatId, message); // Использование MessageService
        logger.debug("Sent no discount message to chatId {}: {}", chatId, message);
    }
}
