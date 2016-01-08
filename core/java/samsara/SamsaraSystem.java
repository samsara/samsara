package samsara;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;
import clojure.java.api.Clojure;
import clojure.lang.IFn;

/**
 * This class it used to bridge the Samza system back
 * to the Clojure pipeline. It is instantiated by
 * Samza runtime via reflection and it receive all
 * messages to process through the process() method.
 * This method just extract the relevant information
 * from the parameters and delegates everything
 * back to Clojure.
 */
public class SamsaraSystem implements StreamTask {


    private final IFn samzaProcess;


    public SamsaraSystem(){
        System.out.println("(*) Initializing SamsaraSystem for Samza:" + Thread.currentThread().getName());
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("samsara-core.samza"));
        samzaProcess = Clojure.var("samsara-core.samza", "samza-process");
    }


    public void process(IncomingMessageEnvelope envelope,
                        MessageCollector collector,
                        TaskCoordinator coordinator) {

        String  message   = (String) envelope.getMessage();
        String  key       = (String) envelope.getKey();
        Integer partition = envelope.getSystemStreamPartition()
                                    .getPartition().getPartitionId();
        String  stream    = envelope.getSystemStreamPartition()
                                    .getSystemStream().getStream();

        //System.out.println("INPUT:[" + stream +"/"+ key + "]:" + message);

        if( ! "".equals(message.trim()) ){
            samzaProcess.invoke( envelope, collector, coordinator,
                                 stream, partition, key, message);
        }
    }
}
