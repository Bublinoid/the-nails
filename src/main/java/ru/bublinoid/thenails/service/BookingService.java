package ru.bublinoid.thenails.service;

import org.springframework.stereotype.Service;
import ru.bublinoid.thenails.telegram.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Service
public class BookingService {

    private final EmailService emailService;

    @Autowired
    public BookingService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void handleEmailInput(long chatId, String email) {
        emailService.handleEmailInput(chatId, email);
    }

    public void confirmEmailCode(long chatId, String code) {
        emailService.confirmEmailCode(chatId, code);
    }
}
