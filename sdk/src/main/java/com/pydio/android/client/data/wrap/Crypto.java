package com.pydio.android.client.data.wrap;

import java.security.NoSuchAlgorithmException;

public class Crypto {
    public static final String HASH_MD5 = "MD5";
    public static final String HASH_SHA1 = "SHA-1";

    private static final String AES_ENCRYPTION_TYPE = "AES";
    private static final String DERIVATION_KEY_ALGO = "PBKDF2WithHmacSHA1";
    private static final String AES_CBC_PADDING = "AES/CBC/PKCS5Padding";

    private static final int DERIVED_KEY_SIZE = 128;
    private static final int DERIVATION_ITERATION = 20000;

    public static byte[] hash(final String algo,  byte[] bytes) throws NoSuchAlgorithmException {
        return com.pydio.sdk.core.security.Crypto.hash(algo, bytes);
    }
    public static String hexHash(final String algo, byte[] bytes) throws NoSuchAlgorithmException {
        return com.pydio.sdk.core.security.Crypto.hexHash(algo, bytes);
    }
}
