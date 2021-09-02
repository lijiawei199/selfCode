package com.doublefs.data.media.management.service.impl.v1.util;

import java.nio.charset.Charset;

public class ByteUtil {
    public static final String bytesToString(byte[] array, Charset charset) {
        return new String(array, charset);
    }
}
