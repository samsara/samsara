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


    private final SystemStream outputStream;
    private final SystemStream kvstoreStream;

    private final IFn dispatch;


    public SamsaraSystem(){
        System.out.println("(*) Initializing SamsaraSystem for Samza");
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("samsara-core.samza"));
        dispatch = Clojure.var("samsara-core.samza", "dispatch");

        // Initialize outputStream to the output-topic
        IFn getOutputTopic = Clojure.var("samsara-core.samza", "output-topic!");
        String outputTopic = (String) getOutputTopic.invoke();
        System.out.println("(*) SamsaraSystem outputTopic: " + outputTopic);
        outputStream = new SystemStream("kafka", outputTopic);

        // Initialize kvstoreStream to the kvstore-topic
        IFn getKVstoreTopic = Clojure.var("samsara-core.samza", "kvstore-topic!");
        String kvstoreTopic = (String) getKVstoreTopic.invoke();
        System.out.println("(*) SamsaraSystem kvstoreTopic: " + kvstoreTopic);
        kvstoreStream = new SystemStream("kafka", kvstoreTopic);
    }


    protected List<List<String>> dispatch( String stream, String key, String event ){
        return (List<List<String>>) dispatch.invoke( stream, key, event );
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
            for( List<String> el : dispatch( stream, partition, message) ) {
                String oStream   = el.get(0);
                String oKey      = el.get(1);
                String output    = el.get(2);

                System.out.println("OUTPUT:[" + oStream +"/"+ oKey + "]:" + output);
                collector.send(new OutgoingMessageEnvelope(outputStream, oKey, output));
            }
        }

    }
}
