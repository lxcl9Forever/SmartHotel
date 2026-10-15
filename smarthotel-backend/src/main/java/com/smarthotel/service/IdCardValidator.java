package com.smarthotel.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class IdCardValidator {
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final String CHECK_DIGITS = "10X98765432";
    private static final DateTimeFormatter BIRTH_DATE = DateTimeFormatter.ofPattern("uuuuMMdd", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private IdCardValidator() {}

    public static boolean isValid(String value) {
        if (value == null || !value.matches("^[1-9]\\d{16}[0-9X]$")) return false;
        try {
            LocalDate.parse(value.substring(6, 14), BIRTH_DATE);
        } catch (DateTimeParseException e) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (value.charAt(i) - '0') * WEIGHTS[i];
        }
        return value.charAt(17) == CHECK_DIGITS.charAt(sum % 11);
    }
}
