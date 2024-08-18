package ru.bublinoid.thenails.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for validating email addresses using a regular expression pattern.
 */
public class EmailValidator {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9]+([._%+-]?[A-Za-z0-9]+)*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
