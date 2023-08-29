package com.chivenh.utils;

import com.chivenh.model.CommitTemplate;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.StringWriter;
import java.util.Properties;

/**
 * @author fulin
 */
public class VelocityUtils {

    private static final VelocityEngine engine;


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

    public static String convert(String template, CommitTemplate commitTemplate,boolean hasFooter) {
        StringWriter writer = new StringWriter();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("type", commitTemplate.getType());
        velocityContext.put("scope", commitTemplate.getScope());
        velocityContext.put("subject", commitTemplate.getSubject());
        velocityContext.put("body", commitTemplate.getBody());
        velocityContext.put("changes", commitTemplate.getChanges());
        velocityContext.put("deprecated", commitTemplate.getDeprecated());
        velocityContext.put("closes", commitTemplate.getCloses());
        velocityContext.put("section", CommitTemplate.SECTION);
        velocityContext.put("breakLine", CommitTemplate.BREAK_LINE);
        velocityContext.put("hasFooter", hasFooter);
        velocityContext.put("velocityTool", new VelocityTool());
        String VM_LOG_TAG = "Leetcode VelocityUtils";
        boolean isSuccess = engine.evaluate(velocityContext, writer, VM_LOG_TAG, template);
        if (!isSuccess) {
        }
        return writer.toString();
    }
}
