package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

/**
 * Provides messages related to the booking process.
 */


@Component
public class BookingInfoProvider {

    public String getRequestEmailMessage() {
        return "Для подтверждения вашей записи, пожалуйста, введите ваш e-mail.\n\n" +
                "Например: example@gmail.com";
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

    public String getInvalidConfirmationCodeFormatMessage() {
        return "Код подтверждения должен состоять из 4 цифр.";
    }
}
