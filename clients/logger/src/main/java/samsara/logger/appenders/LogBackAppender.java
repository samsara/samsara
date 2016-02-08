package samsara.logger.appenders;

import samsara.logger.EventLoggerBuilder;
import samsara.logger.EventLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.Layout;

public class LogBackAppender extends AppenderBase<ILoggingEvent>
{
    private Layout<ILoggingEvent> layout;
    private EventLoggerBuilder builder = new EventLoggerBuilder();
    private EventLogger eventLogger = null;

    public void setLayout(Layout<ILoggingEvent> layout)
    {
        this.layout = layout;
    }

    public Layout<ILoggingEvent> getLayout()
    {
        return this.layout;
    }

    public String getApiUrl()
    {
        return this.builder.getApiUrl();
    }

    public void setApiUrl(String url)
    {
        this.builder.setApiUrl(url);
    }


    @Override
    public void start()
    {
        if(isStarted())
        {
            return;
        }

        if(builder.getApiUrl() == null)
        {
            addError("No apiUrl set.");
            return;
        }

        this.eventLogger = builder.build();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        Level level = eventObject.getLevel();
        String formattedMessage = this.layout.doLayout(eventObject);
        IThrowableProxy throwableProxy = eventObject.getThrowableProxy();

        this.eventLogger.logbackEvent(level, formattedMessage, throwableProxy);
    }


}
