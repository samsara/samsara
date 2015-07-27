package samsara.log4j2;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import samsara.logger.EventLogger;

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
    private AtomicBoolean warnOnce = new AtomicBoolean(false);

    protected SamsaraAppender(String name, Filter filter, Layout<? extends Serializable> layout, String apiUrl, String sourceId, boolean ignoreExceptions) 
    {
        super(name, filter, layout, ignoreExceptions);
        eventLogger = new EventLogger(apiUrl, sourceId);

        if(apiUrl == null || apiUrl.trim().isEmpty())
        {
            warnOnce.set(true);
        }
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

    private void printWarning()
    {
        System.out.println("****************************************************************");
        System.out.println("SAMSARA: The apiUrl property for SamsaraAppender (log4j2.xml) has not been set");
        System.out.println("SAMSARA: Samsara Log4j2 logger will just print to console");
        System.out.println("****************************************************************\n");
    }

    @Override
    public void append(LogEvent event)
    {
        if(warnOnce.getAndSet(false))
        {
            printWarning();
        }

        String message = new String(this.getLayout().toByteArray(event)); 
        eventLogger.log4j2Event(event.getLevel(), message, event.getThrown());
    }
}
