package com.alphnology.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrServiceTest {

    private final QrService qrService = new QrService();

    @Test
    void generatePng_returnsNonEmptyBytes() {
        byte[] result = qrService.generatePng("https://alphnology.com", 200);
        assertThat(result).isNotEmpty();
    }

    @Test
    void generatePng_withShortContent_returnsBytes() {
        byte[] result = qrService.generatePng("hello", 100);
        assertThat(result).isNotEmpty();
    }

    @Test
    void generatePng_withNullContent_throwsRuntimeException() {
        assertThatThrownBy(() -> qrService.generatePng(null, 100))
                .isInstanceOf(RuntimeException.class);
    }
}
