package com.github.microwww.redis.util;

import com.github.microwww.redis.logger.LogFactory;
import com.github.microwww.redis.logger.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class StringUtil {

    private static final Logger logger = LogFactory.getLogger(StringUtil.class);

    public static void printCharBuffer(ByteBuffer bf) {
        if (bf.remaining() > 0) {
            logger.debug(new String(bf.array(), bf.position(), bf.remaining(), StandardCharsets.UTF_8));
        }
    }

    public static String redisErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null) {
            message = message.replaceAll("\\r", "");
        }
        return message;
    }
}
