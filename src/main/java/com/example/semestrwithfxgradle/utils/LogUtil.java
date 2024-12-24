package com.example.semestrwithfxgradle.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
    public static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    public static void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    public static void info(String format, Object... args) {
        logger.info(format, args);
    }

    public static void error(String format, Throwable throwable) {
        logger.error(format, throwable);
    }
}