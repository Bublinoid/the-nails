package ru.bublinoid.thenails.utils;

import java.security.SecureRandom;

/**
 * Utility class for generating secure random codes.
 */
public class CodeGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static int generateFourDigitCode() {
        return 1000 + random.nextInt(9000);
    }

}