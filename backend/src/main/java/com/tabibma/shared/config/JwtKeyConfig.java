package com.tabibma.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RS256 keypair for JWT signing, per docs/security-tabib-ma.md Section 3.
 * Inactive under the "test" profile — see com.tabibma.testsupport.TestJwtKeyConfig,
 * which generates an ephemeral keypair instead of requiring committed key material.
 */
@Configuration
@Profile("!test")
public class JwtKeyConfig {

    @Bean
    public PrivateKey jwtPrivateKey(@Value("${app.jwt.private-key-b64}") String privateKeyB64) throws Exception {
        byte[] der = pemToDer(privateKeyB64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    @Bean
    public PublicKey jwtPublicKey(@Value("${app.jwt.public-key-b64}") String publicKeyB64) throws Exception {
        byte[] der = pemToDer(publicKeyB64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /** Decodes the outer (env-var) base64 to get PEM text, strips the header/footer/whitespace, then decodes the inner base64 to raw DER bytes. */
    private byte[] pemToDer(String outerBase64) {
        String pemText = new String(Base64.getDecoder().decode(outerBase64), StandardCharsets.UTF_8);
        String innerBase64 = pemText
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(innerBase64);
    }
}
