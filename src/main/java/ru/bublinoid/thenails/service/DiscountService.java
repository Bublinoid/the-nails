package ru.bublinoid.thenails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDice;
import org.telegram.telegrambots.meta.api.objects.Dice;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bublinoid.thenails.telegram.TelegramBot;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);

    private final TelegramBot telegramBot;
    private final MessageService messageService;

    // Хранение времени последней игры пользователя
    private final Map<Long, LocalDate> lastPlayedDateMap = new HashMap<>();

    @Autowired
    public DiscountService(@Lazy TelegramBot telegramBot, MessageService messageService) {
        this.telegramBot = telegramBot;
        this.messageService = messageService;
    }

    public void playDiscountGame(Long chatId) {
        // Проверка, играл ли пользователь сегодня
        if (hasPlayedToday(chatId)) {
            String message = "Вы уже играли сегодня. Попробуйте снова завтра!";
            messageService.sendMarkdownMessage(chatId, message);
            logger.info("User with chatId {} already played today.", chatId);
            return;
        }

        // Бросаем первый кубик
        int firstDiceValue = rollDice(chatId);
        if (firstDiceValue == -1) {
            return;
        }

        // Бросаем второй кубик
        int secondDiceValue = rollDice(chatId);
        if (secondDiceValue == -1) {
            return;
        }

        // Обновляем время последней игры
        lastPlayedDateMap.put(chatId, LocalDate.now());

        // Проверяем, совпадают ли значения
        if (firstDiceValue == secondDiceValue) {
            sendDiscountWinMessage(chatId, firstDiceValue, secondDiceValue);
        } else {
            sendNoDiscountMessage(chatId, firstDiceValue, secondDiceValue);
        }
    }

    private boolean hasPlayedToday(Long chatId) {
        LocalDate lastPlayedDate = lastPlayedDateMap.get(chatId);
        return lastPlayedDate != null && lastPlayedDate.equals(LocalDate.now());
    }

    private int rollDice(Long chatId) {
        SendDice sendDice = new SendDice();
        sendDice.setChatId(chatId);
        sendDice.setEmoji("🎲");  // Игральная кость

        try {
            Message message = telegramBot.execute(sendDice); // Отправка анимации игральной кости
            return message.getDice().getValue();
        } catch (TelegramApiException e) {
            logger.error("Error occurred while sending dice: ", e);
            return -1; // Возвращаем -1 в случае ошибки
        }
    }

    private void sendDiscountWinMessage(Long chatId, int firstDiceValue, int secondDiceValue) {
        String message = "Поздравляем! Вам выпало два кубика с одинаковым значением " + firstDiceValue +
                ". Вы выиграли скидку 15%!";
        messageService.sendMarkdownMessage(chatId, message); // Использование MessageService
        logger.info("Sent discount win message to chatId {}: {}", chatId, message);
    }

    private void sendNoDiscountMessage(Long chatId, int firstDiceValue, int secondDiceValue) {
        String message = "Вам выпало " + firstDiceValue + " и " + secondDiceValue +
                ". К сожалению, скидку выиграть не удалось. Попробуйте через 24 часа!";
        messageService.sendMarkdownMessage(chatId, message); // Использование MessageService
        logger.info("Sent no discount message to chatId {}: {}", chatId, message);
    }
}
