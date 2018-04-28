package com.disapp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Properties {
    private static final Logger logger = LoggerFactory.getLogger(Properties.class);

    private static final String FILE_PROPERTIES = "params.properties";

    private static java.util.Properties properties;

    private Properties() {}

    static {
        properties = new java.util.Properties();
        try {
            logger.info(FILE_PROPERTIES + " is loading from classpath");
            properties.load(Properties.class.getClassLoader().getResourceAsStream(FILE_PROPERTIES));
            logger.info(FILE_PROPERTIES + " loaded");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static java.util.Properties getProperties() {
        return Properties.properties;
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static long getLong(String key) throws NumberFormatException {
        return Long.valueOf(getString(key));
    }

    public static int getInteger(String key) throws NumberFormatException {
        return Integer.valueOf(getString(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key));
    }
}
