package ru.bublinoid.thenails.service;

import org.springframework.stereotype.Service;
import ru.bublinoid.thenails.model.Email;
import ru.bublinoid.thenails.repository.EmailRepository;
import ru.bublinoid.thenails.telegram.TelegramBot;
import ru.bublinoid.thenails.utils.CodeGenerator;
import ru.bublinoid.thenails.utils.EmailSender;
import ru.bublinoid.thenails.utils.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Service
public class BookingService {

    private final EmailRepository emailRepository;
    private final TelegramBot telegramBot;
    private final EmailSender emailSender;
    private final Map<Long, String> userEmails = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    public BookingService(EmailRepository emailRepository, @Lazy TelegramBot telegramBot, EmailSender emailSender) {
        this.emailRepository = emailRepository;
        this.telegramBot = telegramBot;
        this.emailSender = emailSender;
    }

    public void handleEmailInput(long chatId, String email) {
        if (EmailValidator.isValid(email)) {
            // Создание и сохранение объекта Email
            Email emailEntity = new Email();
            emailEntity.setChatId(chatId);
            emailEntity.setEmail(email);

            UUID hash = emailEntity.generateHash(); // Генерация хэша
            emailEntity.setHash(hash);

            Optional<Email> existingEmail = emailRepository.findByHash(hash);
            if (existingEmail.isPresent()) {
                logger.info("Email уже подтвержден для chatId: {}", chatId);
                telegramBot.sendEmailAlreadyConfirmedMessage(chatId);
            } else {
                userEmails.put(chatId, email);
                logger.info("Получен действительный email: {} от chatId: {}", email, chatId);

                // Генерация и сохранение кода подтверждения
                String confirmationCode = String.format("%04d", CodeGenerator.generateFourDigitCode());
                emailEntity.setConfirmationCode(confirmationCode);

                emailRepository.save(emailEntity);

                logger.info("Email и код подтверждения сохранены в базе данных: {} для chatId: {}", email, chatId);

                // Отправка email с кодом подтверждения
                String subject = "Ваш код подтверждения";
                String content = buildConfirmationEmailContent(confirmationCode);
                emailSender.sendEmail(email, subject, content);

                telegramBot.sendEmailSavedMessage(chatId);
            }
        } else {
            logger.warn("Получен недействительный email: {} от chatId: {}", email, chatId);
            telegramBot.sendInvalidEmailMessage(chatId);
        }
    }

    private String buildConfirmationEmailContent(String confirmationCode) {
        return "<!DOCTYPE html>" +
                "<html lang=\"ru\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Подтверждение email</title>" +
                "    <style>" +
                "        body {" +
                "            font-family: Arial, sans-serif;" +
                "            background-color: #f4f4f9;" +
                "            color: #333;" +
                "        }" +
                "        .container {" +
                "            width: 80%;" +
                "            margin: auto;" +
                "            padding: 20px;" +
                "            background-color: #ffffff;" +
                "            border-radius: 8px;" +
                "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);" +
                "        }" +
                "        .header {" +
                "            text-align: center;" +
                "            padding-bottom: 20px;" +
                "        }" +
                "        .content {" +
                "            font-size: 16px;" +
                "            line-height: 1.6;" +
                "        }" +
                "        .code {" +
                "            font-size: 24px;" +
                "            font-weight: bold;" +
                "            color: #d9534f;" +
                "            text-align: center;" +
                "            margin: 20px 0;" +
                "        }" +
                "        .footer {" +
                "            text-align: center;" +
                "            font-size: 12px;" +
                "            color: #aaa;" +
                "            margin-top: 20px;" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h1>Подтверждение email</h1>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>Здравствуйте,</p>" +
                "            <p>Спасибо за регистрацию! Пожалуйста, используйте следующий код для подтверждения вашего email:</p>" +
                "            <div class=\"code\">" + confirmationCode + "</div>" +
                "            <p>Если вы не регистрировались у нас, пожалуйста, проигнорируйте это сообщение.</p>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>С уважением,<br>Команда The Nails</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    public void confirmEmailCode(long chatId, String code) {
        Optional<Email> emailEntityOptional = emailRepository.findByChatId(chatId);
        if (emailEntityOptional.isPresent()) {
            Email emailEntity = emailEntityOptional.get();
            if (emailEntity.getConfirmationCode().equals(code)) {
                logger.info("Код подтверждения верный для chatId: {}", chatId);
                telegramBot.sendEmailConfirmedMessage(chatId);
            } else {
                logger.warn("Неверный код подтверждения для chatId: {}", chatId);
                telegramBot.sendInvalidConfirmationCodeMessage(chatId);
            }
        } else {
            logger.warn("Не найден email для chatId: {}", chatId);
        }
    }
}
