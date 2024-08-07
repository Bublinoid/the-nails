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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
            UUID hash = generateHash(chatId, email);
            Optional<Email> existingEmail = emailRepository.findByHash(hash);
            if (existingEmail.isPresent()) {
                logger.info("Email уже подтвержден для chatId: {}", chatId);
                telegramBot.sendEmailAlreadyConfirmedMessage(chatId);
            } else {
                userEmails.put(chatId, email);
                logger.info("Получен действительный email: {} от chatId: {}", email, chatId);

                // Генерация и сохранение кода подтверждения
                String confirmationCode = String.format("%04d", CodeGenerator.generateFourDigitCode());
                Email emailEntity = Email.builder()
                        .hash(hash)
                        .chatId(chatId)
                        .email(email)
                        .confirmationCode(confirmationCode)
                        .build();
                emailRepository.save(emailEntity); // хэш будет обновлен автоматически

                logger.info("Email и код подтверждения сохранены в базе данных: {} для chatId: {}", email, chatId);

                // Отправка email с кодом подтверждения
                String subject = "Ваш код подтверждения";
                String content = "Ваш код подтверждения: " + confirmationCode;
                emailSender.sendEmail(email, subject, content);

                telegramBot.sendEmailSavedMessage(chatId);
            }
        } else {
            logger.warn("Получен недействительный email: {} от chatId: {}", email, chatId);
            telegramBot.sendInvalidEmailMessage(chatId);
        }
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

    private UUID generateHash(Long chatId, String email) {
        String hashFieldValues = chatId.toString() + email.toUpperCase();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hashFieldValues.getBytes(Charset.forName("UTF-8")));

            ByteBuffer byteBuffer = ByteBuffer.wrap(md.digest());
            long high = byteBuffer.getLong();
            long low = byteBuffer.getLong();
            return new UUID(high, low);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 provider is required on your JVM");
        }
    }
}
