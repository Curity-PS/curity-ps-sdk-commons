package io.curity.identityserver.plugins.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.jose4j.lang.HashUtil.SHA_256;
import static org.jose4j.lang.HashUtil.getMessageDigest;

/**
 * Utility class for PKCE (Proof Key for Code Exchange) related operations.
 */
public final class PkceHelper
{
    private PkceHelper()
    {
    }

    /**
     * Generates a code challenge from the provided code verifier using SHA-256 hashing.
     *
     * @param codeVerifier the code verifier
     * @return the code challenge
     */
    public static String challengeFromVerifier(String codeVerifier)
    {
        MessageDigest messageDigest = getMessageDigest(SHA_256);
        byte[] digest = messageDigest.digest(codeVerifier.getBytes(US_ASCII));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    /**
     * Generates a code verifier using a secure random number generator.
     *
     * @return the code verifier
     */
    public static String generateCodeVerifier()
    {
        int codeVerifierLength = 128;
        char[] allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789.-_~".toCharArray();
        int allAllowedLength = allAllowed.length;
        Random random = new SecureRandom();
        StringBuilder codeVerifier = new StringBuilder();

        for (int i = 0; i < codeVerifierLength; i++)
        {
            codeVerifier.append(allAllowed[random.nextInt(allAllowedLength)]);
        }

        return codeVerifier.toString();
    }
}
