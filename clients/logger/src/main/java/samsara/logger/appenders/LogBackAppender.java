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

    public String getAppId()
    {
        return this.builder.getAppId();
    }

    public void setAppId(String appId)
    {
        this.builder.setAppId(appId);
    }

    public String getSourceId()
    {
        return this.builder.getSourceId();
    }

    public void setSourceId(String sourceId)
    {
        this.builder.setSourceId(sourceId);
    }

    public long getPublishInterval()
    {
        return this.builder.getPublishInterval();
    }

    public void setPublishInterval(long interval)
    {
        this.builder.setPublishInterval(interval);
    }

    public long getMinBufferSize()
    {
        return this.builder.getMinBufferSize();
    }

    public void setMinBufferSize(long bufferSize)
    {
        this.builder.setMinBufferSize(bufferSize);
    }

    public long getMaxBufferSize()
    {
        return this.builder.getMaxBufferSize();
    }

    public void setMaxBufferSize(long bufferSize)
    {
        this.builder.setMaxBufferSize(bufferSize);
    }

    public String getCompression()
    {
        return this.builder.getCompression();
    }

    public void setCompression(String compressionType)
    {
        this.builder.setCompression(compressionType);
    }

    public String getServiceName()
    {
        return this.builder.getServiceName();
    }

    public void setServiceName(String serviceName)
    {
        this.builder.setServiceName(serviceName);
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
