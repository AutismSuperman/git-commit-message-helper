package com.fulinlin.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PropertiesUtils {

    private final static String baseName = "i18n/info";
    private final static ResourceBundle rb1 = ResourceBundle.getBundle(baseName);

    public static String getInfo(String key, String... params) {
        return new MessageFormat(rb1.getString(key)).format(params);
    }

}
