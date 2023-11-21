package com.fulinlin.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;


@Deprecated
public class I18nUtil {

    private final static String baseName = "i18n/info";
    private final static ResourceBundle rb1 = ResourceBundle.getBundle(baseName);

    public static String getInfo(String key, String... params) {
        String string = rb1.getString(key);
        string = string.replace("${", "$'{'");
        string = string.replace("}", "'}'");
        return new MessageFormat(string).format(params);
    }

}
