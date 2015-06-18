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

public class SamsaraSystem implements StreamTask {


    private final IFn samzaProcess;


    public SamsaraSystem(){
        System.out.println("(*) Initializing SamsaraSystem for Samza");
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("samsara-core.samza"));
        samzaProcess = Clojure.var("samsara-core.samza", "samza-process");
    }


    public void process(IncomingMessageEnvelope envelope,
                        MessageCollector collector,
                        TaskCoordinator coordinator) {

        String message   = (String) envelope.getMessage();
        String partition = (String) envelope.getKey();
        String stream    = envelope.getSystemStreamPartition()
                                   .getSystemStream().getStream();

        System.out.println("INPUT:[" + stream +"/"+ partition + "]:" + message);

        if( ! "".equals(message.trim()) ){
            samzaProcess.invoke( envelope, collector, coordinator,
                                 stream, partition, message);
        }
    }
}
