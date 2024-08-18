package ru.bublinoid.thenails.content;

import org.springframework.stereotype.Component;

/**
 * Provides information about the discount offer.
 */

@Component
public class DiscountInfoProvider {

    public String getDiscountInfo() {
        return "Вам предоставлена возможность выиграть скидку *15%*.\n" +
                "Условия: при нажатии кнопки *играть* выкинутся два кубика. " +
                "Если значения совпадут, Вы выиграете скидку 15%, если нет, то повезет в другой раз.\n" +
                "Если Вы готовы, нажмите кнопку *играть!*";
    }

}
