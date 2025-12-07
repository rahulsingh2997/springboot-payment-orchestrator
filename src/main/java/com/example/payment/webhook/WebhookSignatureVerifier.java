package com.example.payment.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WebhookSignatureVerifier {

    @Value("${authorize-net.webhook.signature-key:}")
    private String signatureKeyHex;

    /**
     * Verify Authorize.Net signature header of the form: SHA512=<hexDigest>
     * Returns true/false and must NOT throw.
     */
    public boolean verify(String payload, String headerSignature) {
        if (signatureKeyHex == null || signatureKeyHex.isEmpty()) return false;
        if (headerSignature == null || headerSignature.isEmpty()) return false;

        try {
            // header format: SHA512=<hex>
            String hs = headerSignature.trim();
            int eq = hs.indexOf('=');
            String algPart = eq > 0 ? hs.substring(0, eq) : hs;
            String sigHex = eq > 0 ? hs.substring(eq + 1) : "";
            if (!algPart.equalsIgnoreCase("SHA512") || sigHex.isEmpty()) return false;

            // decode signature key hex -> bytes
            byte[] keyBytes = hexDecode(signatureKeyHex.trim());
            if (keyBytes == null || keyBytes.length == 0) return false;

            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // decode provided signature hex into bytes and compare raw bytes
            byte[] providedBytes = hexDecode(sigHex.trim());
            if (providedBytes == null || providedBytes.length != digest.length) return false;

            // constant-time compare raw bytes
            return MessageDigest.isEqual(digest, providedBytes);
        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] hexDecode(String hex) {
        if (hex == null) return null;
        String s = hex.trim();
        if (s.length() % 2 != 0) return null;
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) return null;
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
