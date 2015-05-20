package samsara.log4j2;

import java.io.Serializable;

import samsara.log4j2.SamsaraLogger;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LogEvent;


@Plugin(name = "SamsaraAppender", category = "Core", elementType = "appender", printObject = true)
public class SamsaraAppender extends AbstractAppender
{
    private SamsaraLogger samsaraLogger;

    protected SamsaraAppender(String name, Filter filter, Layout<? extends Serializable> layout, String apiUrl, String sourceId, boolean ignoreExceptions) 
    {
        super(name, filter, layout, ignoreExceptions);
        samsaraLogger = new SamsaraLogger(apiUrl, sourceId);
    }

    @PluginFactory
    public static SamsaraAppender createAppender(@PluginAttribute("name") String name,
                                                 @PluginAttribute("apiUrl") String apiUrl,
                                                 @PluginAttribute("sourceId") String sourceId,
                                                 @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                 @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                 @PluginElement("Filter") Filter filter)
    {
        if(name == null)
        {
            LOGGER.error("No name provided for SamsaraAppender");
            return null;
        }

        if(layout == null)
        {
            layout = PatternLayout.createDefaultLayout();
        }

        return new SamsaraAppender(name, filter, layout, apiUrl, sourceId, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event)
    {
        String message = new String(this.getLayout().toByteArray(event)); 
        samsaraLogger.logEvent(event.getLevel(), message, event.getThrown());
    }
}
