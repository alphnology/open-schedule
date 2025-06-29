package com.alphnology.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author me@fredpena.dev
 * @created 03/05/2025  - 15:25
 */
public class RegexGenerator {

    private static final Random random = new Random();

    private RegexGenerator(){}

    public static String generateMatchingString(int length) {
        if (length < 5) throw new IllegalArgumentException("Length must be at least 5");

        List<Character> chars = new ArrayList<>();

        String all = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        chars.add(randomChar(all));

        chars.add(randomChar("0123456789"));

        chars.add(randomChar("(!@#$%&*()_+.)"));

        while (chars.size() < length) {
            chars.add(randomChar(all));
        }

        Collections.shuffle(chars);

        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);

        return sb.toString();
    }

    private static char randomChar(String candidates) {
        return candidates.charAt(random.nextInt(candidates.length()));
    }
}
