package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

/**
 * Provides information about the available services.
 */

@Component
public class ServicesInfoProvider {

    public String getServicesInfo() {
        return "*Маникюр*\n" +
                "- классический/ аппаратный/ комбинированный\n\n" +
                "*Пилочный маникюр*\n" +
                "- максимально быстрая, качественная и безопасная техника\n" +
                "- самый безопасный и безболезненный способ ухода за кутикулой\n\n" +
                "*Комплекс 1 \"Маникюр с покрытием гель-лак\"*\n" +
                "- классический/ аппаратный/ комбинированный\n" +
                "- снятие\n" +
                "- покрытие гель-лаком: Beautix/ Luxio\n" +
                "- нанесение крема\n" +
                "- масло для кутикулы";
    }
}
