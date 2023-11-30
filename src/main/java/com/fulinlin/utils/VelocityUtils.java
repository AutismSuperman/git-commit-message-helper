package com.fulinlin.utils;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.StringWriter;
import java.util.Properties;

/**
 * @author fulin
 */
public class VelocityUtils {

    private static VelocityEngine engine;


    static {
        engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.PARSER_POOL_SIZE, 20);
        engine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        engine.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8");

        Properties props = new Properties();
        props.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        props.put("runtime.log.logsystem.log4j.category", "velocity");
        props.put("runtime.log.logsystem.log4j.logger", "velocity");

        engine.init(props);
    }

    public static String convert(String template, CommitTemplate commitTemplate) {
        StringWriter writer = new StringWriter();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("type", commitTemplate.getType());
        velocityContext.put("scope", commitTemplate.getScope());
        velocityContext.put("subject", commitTemplate.getSubject());
        velocityContext.put("body", commitTemplate.getBody());
        velocityContext.put("changes", commitTemplate.getChanges());
        velocityContext.put("closes", commitTemplate.getCloses());
        velocityContext.put("newline", "\n");
        velocityContext.put("velocityTool", new VelocityTool());
        String VM_LOG_TAG = "GitCommitMessage VelocityUtils";
        boolean isSuccess = engine.evaluate(velocityContext, writer, VM_LOG_TAG, template);
        if (!isSuccess) {
        }
        return writer.toString();
    }



    public static String convertDescription(String html) {
        StringWriter writer = new StringWriter();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("setting.template.description.tip", PluginBundle.get("setting.template.description.tip"));
        velocityContext.put("setting.template.description.predefined.tip", PluginBundle.get("setting.template.description.predefined.tip"));
        velocityContext.put("setting.template.type", PluginBundle.get("setting.template.type"));
        velocityContext.put("setting.template.scope", PluginBundle.get("setting.template.scope"));
        velocityContext.put("setting.template.subject", PluginBundle.get("setting.template.subject"));
        velocityContext.put("setting.template.body", PluginBundle.get("setting.template.body"));
        velocityContext.put("setting.template.changes", PluginBundle.get("setting.template.changes"));
        velocityContext.put("setting.template.closes", PluginBundle.get("setting.template.closes"));
        velocityContext.put("setting.template.newLine", PluginBundle.get("setting.template.newLine"));
        velocityContext.put("setting.template.used", PluginBundle.get("setting.template.used"));
        velocityContext.put("globals",velocityContext);
        String VM_LOG_TAG = "GitCommitMessage VelocityUtils";
        boolean isSuccess = engine.evaluate(velocityContext, writer, VM_LOG_TAG, html);
        if (!isSuccess) {
        }
        return writer.toString();
    }
}
