package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

@Component
public class AboutUsInfoProvider {

    public String getAboutUsInfo() {
        return "Мы – студия маникюра *THE NAILS*, предлагаем высококачественные услуги по уходу за ногтями и кожей рук и ног. " +
                "Наша команда состоит из опытных мастеров, которые используют только лучшие материалы и инструменты. " +
                "Мы гарантируем индивидуальный подход к каждому клиенту и высокие стандарты гигиены. " +
                "Приходите к нам, чтобы получить профессиональный уход и наслаждаться красивыми ногтями!";
    }
}
