package com.doublefs.data.media.management.service.impl.v1.util;

import com.doublefs.data.common.util.MessageDigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.security.NoSuchAlgorithmException;

public class GeneratorUniqueKeyUtil {
    private static Logger LOGGER = LoggerFactory.getLogger(GeneratorUniqueKeyUtil.class);
    public static String md5(Object... objs) {
        try {
            StringBuffer sb = new StringBuffer();
            if (!ObjectUtils.isEmpty(objs)) {
                for (Object obj : objs) {
                    if (obj instanceof String) {
                        sb.append(((String) obj).trim()).append(",");
                    } else if (obj instanceof Long) {
                        Long o = (Long) obj;
                        if (o == 0L) {
                            sb.append("0").append(",");
                        } else {
                            sb.append(String.valueOf(obj)).append(",");
                        }
                    } else if (obj instanceof Integer) {
                        Integer o = (Integer) obj;
                        if (o == 0) {
                            sb.append("0").append(",");
                        } else {
                            sb.append(String.valueOf(obj)).append(",");
                        }
                    } else if (obj instanceof Byte) {
                        Byte o = (Byte) obj;
                        if (o == 0) {
                            sb.append("0").append(",");
                        } else {
                            sb.append(String.valueOf(obj)).append(",");
                        }
                    }else {
                        sb.append(String.valueOf(obj)).append(",");
                    }
                }
            }
            String s = sb.toString();
            if (s.length() > 0 && s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
            }
            String uniqueKey = MessageDigestUtil.md5(s);
            return uniqueKey;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("GeneratorUniqueKeyUtil md5  {}",e);
        }
        return "";
    }
    
}
