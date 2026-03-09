package com.snaplink.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    public String encode(long id) {
        if (id == 0) {
            return "0";
        }
        StringBuilder result = new StringBuilder();
        while (id > 0) {
            result.insert(0, ALPHABET.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return result.toString();
    }

    public long decode(String shortCode) {
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid character in short code: " + c);
            }
            id = id * BASE + index;
        }
        return id;
    }
}
