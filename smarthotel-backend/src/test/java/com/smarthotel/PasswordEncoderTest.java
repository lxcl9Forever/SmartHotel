package com.smarthotel;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderTest {

    @Test
    void encodePassword() {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String staffPassword =
                encoder.encode("staff123");

        String customerPassword =
                encoder.encode("customer123");

        System.out.println("staff123:");
        System.out.println(staffPassword);

        System.out.println("customer123:");
        System.out.println(customerPassword);
    }
}