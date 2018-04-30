package com.disapp.utils;

import com.disapp.annotations.InitClass;
import com.disapp.annotations.PropertyFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;

@InitClass
public class Properties {
    private static final Logger logger = LoggerFactory.getLogger(Properties.class);

    public interface FileSystem {
        String DEFAULT_SEPARATOR  = FileSystems.getDefault().getSeparator();
        String APPLICATION_DIRECTORY = System.getProperty("user.dir");
        String SETTINGS_DIRECTORY = APPLICATION_DIRECTORY + DEFAULT_SEPARATOR + "settings";
        String ICONS_DIRECTORY = APPLICATION_DIRECTORY + DEFAULT_SEPARATOR + "icons";
    }

    public interface Files {
        @PropertyFile(fileName = "params.properties")
        java.util.Properties params = new java.util.Properties();

        @PropertyFile(fileName = "emoji.properties")
        java.util.Properties emoji = new java.util.Properties();

        @PropertyFile(fileName = "phrases.properties")
        java.util.Properties phrases = new java.util.Properties();

        @PropertyFile(fileName = "regexp.properties")
        java.util.Properties regexp = new java.util.Properties();
    }

    public static void init(){}

    private Properties() {}

    static {
        for (Field field : Files.class.getFields()) {
            PropertyFile column = field.getAnnotation(PropertyFile.class);
            Class<?> fieldClass = field.getType();
            if (column != null && fieldClass.isAssignableFrom(java.util.Properties.class)) {
                File prop = new File(FileSystem.SETTINGS_DIRECTORY + FileSystem.DEFAULT_SEPARATOR + column.fileName());
                try {
                    Method load = fieldClass.getMethod("load", Reader.class);
                    if (prop.exists() && prop.isFile()) {
                        logger.info(column.fileName() + " is loading from " + prop.getPath());
                        load.invoke(
                                field.get(java.util.Properties.class),
                                new InputStreamReader(new FileInputStream(prop))
                        );
                    }
                    else {
                        logger.info(column.fileName() + " is loading from classpath");
                        load.invoke(
                                field.get(java.util.Properties.class),
                                new InputStreamReader(Properties.class.getClassLoader().getResourceAsStream(column.fileName()))
                        );
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                logger.info(column.fileName() + " loaded");
            }
        }
    }

    public static String getProperty (String key) {
        for (Field field : Files.class.getFields()) {
            Class<?> fieldClass = field.getType();
            try {
                Method getProperty = fieldClass.getMethod("getProperty", String.class);
                String property = (String) getProperty.invoke(field.get(java.util.Properties.class), key);
                if (property != null)
                    return property;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    public static String getEmoji(String key) {
        return Files.emoji.getProperty(key);
    }

    public static String getPhrase(String key) {
        String phrase = Files.phrases.getProperty(key);
        return phrase == null ? Files.phrases.getProperty("default.phrase") : phrase;
    }

    public static String getRegexp(String key) {
        return Files.regexp.getProperty(key);
    }
}
