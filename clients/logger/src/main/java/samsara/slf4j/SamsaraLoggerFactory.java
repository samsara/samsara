package samsara.slf4j;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;

public class SamsaraLoggerFactory implements ILoggerFactory
{
    private ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    public Logger getLogger(String name)
    {
        Logger samsaraLogger = loggerMap.get(name);
        if(samsaraLogger != null)
        {
            return samsaraLogger;
        }
        else
        {
            Logger newLogger = new SamsaraLogger(name);
            Logger oldLogger = loggerMap.putIfAbsent(name, newLogger);
            return oldLogger == null ? newLogger : oldLogger;
        }
    }
}

