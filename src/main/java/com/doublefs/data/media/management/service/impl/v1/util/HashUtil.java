package com.doublefs.data.media.management.service.impl.v1.util;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    public static String md5(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();

        return String.valueOf(Hex.encodeHex(digest));
    }
}
