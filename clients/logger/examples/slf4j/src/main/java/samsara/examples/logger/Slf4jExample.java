package samsara.examples.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jExample
{
    private static final Logger log = LoggerFactory.getLogger(Slf4jExample.class);

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
        private final Logger homerLog = LoggerFactory.getLogger(Homer.class);

        Homer()
        {
            homerLog.info("Homer created");
        }
    }

    private static class Marge
    {
        private final Logger margeLog = LoggerFactory.getLogger(Marge.class);

        Marge()
        {
            margeLog.info("Marge created");
        }
    }

    private static class Bart
    {
        private final Logger bartLog = LoggerFactory.getLogger(Bart.class);

        Bart()
        {
            bartLog.info("Bart created");
        }
    }

    private static class Lisa
    {
        private final Logger lisaLog = LoggerFactory.getLogger(Lisa.class);

        Lisa()
        {
            lisaLog.info("Lisa created");
        }
    }

    private static class Maggie
    {
        private final Logger maggieLog = LoggerFactory.getLogger(Maggie.class);

        Maggie()
        {
            maggieLog.info("Maggie created");
        }
    }
}
