package samsara.sl4j;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;

public class SamsaraLoggerFactory implements ILoggerFactory
{
    private ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    public Logger getLogger(String name)
    {
        return loggerMap.putIfAbsent(name, new SamsaraLogger(name));
    }
}
