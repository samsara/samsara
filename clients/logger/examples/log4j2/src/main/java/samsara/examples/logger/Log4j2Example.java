package samsara.examples.logger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Log4j2Example
{
    private static final Logger log = LogManager.getLogger(Log4j2Example.class);

    public static void main(String[] args)
    {
        log.debug("Main method entered");

        log.info("Creating the Simpsons");
        new Homer();
        new Marge();
        new Bart();
        new Lisa();
        new Maggie();
        log.info("Simpsons created");

        log.debug("Main method exited");
    }


    private static class Homer
    {
        private final Logger homerLog = LogManager.getLogger(Homer.class);

        Homer()
        {
            homerLog.info("Homer created");
        }
    }

    private static class Marge
    {
        private final Logger margeLog = LogManager.getLogger(Marge.class);

        Marge()
        {
            margeLog.info("Marge created");
        }
    }

    private static class Bart
    {
        private final Logger bartLog = LogManager.getLogger(Bart.class);

        Bart()
        {
            bartLog.info("Bart created");
        }
    }

    private static class Lisa
    {
        private final Logger lisaLog = LogManager.getLogger(Lisa.class);

        Lisa()
        {
            lisaLog.info("Lisa created");
        }
    }

    private static class Maggie
    {
        private final Logger maggieLog = LogManager.getLogger(Maggie.class);

        Maggie()
        {
            maggieLog.info("Maggie created");
        }
    }
}
