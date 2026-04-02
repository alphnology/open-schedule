package com.alphnology.services.email;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class MailSecretCodec {

    private static final String PREFIX = "{aes-gcm}";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final MailSettingsProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean canPersistSecrets() {
        return properties.isAllowUiSecretPersistence() && StringUtils.hasText(properties.getSettingsMasterKey());
    }

    public String encrypt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        if (!canPersistSecrets()) {
            throw new IllegalStateException(
                    "UI secret persistence requires application.email.settings-master-key and allow-ui-secret-persistence=true");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt mail secret", ex);
        }
    }

    @Nullable
    public String decrypt(@Nullable String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return null;
        }
        if (!encrypted.startsWith(PREFIX)) {
            return encrypted;
        }
        if (!StringUtils.hasText(properties.getSettingsMasterKey())) {
            throw new IllegalStateException("Mail settings master key is required to decrypt persisted secrets");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to decrypt mail secret", ex);
        }
    }

    private SecretKey buildKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(properties.getSettingsMasterKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
