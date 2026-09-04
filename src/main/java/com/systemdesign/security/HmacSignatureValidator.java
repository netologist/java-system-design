package com.systemdesign.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC Constant-Time Signature Verification (Category 19).
 * <p>
 * Defends webhook and API handlers against <b>Timing Attacks</b> by ensuring
 * signature comparison always takes constant time via {@link MessageDigest#isEqual}.
 */
public final class HmacSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(HmacSignatureValidator.class);

    private HmacSignatureValidator() {}

    public static boolean isValidHmac(byte[] payload, String expectedSignature, String secret) {
        if (payload == null || expectedSignature == null || secret == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(payload);
            String calculatedHex = HexFormat.of().formatHex(rawHmac);

            // MessageDigest.isEqual() guarantees constant-time comparison!
            return MessageDigest.isEqual(
                    calculatedHex.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("Failed to compute HMAC: {}", ex.getMessage());
            return false;
        }
    }
}
