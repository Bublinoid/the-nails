package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

@Component
public class BookingInfoProvider {

    public String getRequestEmailMessage() {
        return "Для подтверждения вашей записи, пожалуйста, введите ваш e-mail.\n\n" +
                "Например: example@gmail.com";
    }

    public String getVerificationCodeMessage() {
        return "На ваш e-mail был отправлен код подтверждения. Пожалуйста, введите этот код для подтверждения.";
    }

    public String getEmailConfirmedMessage() {
        return "Ваш e-mail успешно подтвержден! Мы готовы принять вашу запись.";
    }

    public String getInvalidCodeMessage() {
        return "Неверный код подтверждения. Пожалуйста, попробуйте снова.";
    }

    public String getInvalidEmailMessage() {
        return "Введен неверный адрес электронной почты. Пожалуйста, попробуйте снова.";
    }

    public String getEmailSavedMessage() {
        return "Вам отправлен код подтверждения на вашу почту. Пожалуйста, введите этот код здесь.";
    }
}
