package com.fulinlin.ui.setting.description;

import b.j.P;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.utils.VelocityTool;
import com.fulinlin.utils.VelocityUtils;
import org.apache.velocity.VelocityContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public class DescriptionRead {
    public static String readHtmlFile() {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = DescriptionRead.class.getResourceAsStream("/includes/defaultDescription.html")) {
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return VelocityUtils.convertDescription(content.toString());
    }


}
