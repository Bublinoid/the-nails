package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

@Component
public class ContactsInfoProvider {

    public String getContactsInfo() {
        return "Наши контакты:\n\n" +
                "*Адрес:*\n" +
                "г. Москва, ул. Примерная, д. 1\n\n" +
                "*Телефон:*\n" +
                "+7 (123) 456-78-90\n\n" +
                "*Email:*\n" +
                "info@thenails.ru\n\n" +
                "*Часы работы:*\n" +
                "Понедельник - Пятница: 09:00 - 21:00\n" +
                "Суббота - Воскресенье: 10:00 - 18:00";
    }
}
