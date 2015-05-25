package samsara;

import java.util.HashMap;
import java.util.Map;
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


    private final SystemStream OUTPUT_STREAM = new SystemStream("kafka", "messages");

    private final IFn pipeline;


    public SamsaraSystem(){
        System.out.println("(*) Initializing SamsaraSystem for Samza");
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("samsara-core.samza"));
        pipeline = Clojure.var("samsara-core.samza", "pipeline");
    }


    protected String pipeline( String event ){
        return (String)pipeline.invoke( event );
    }


    public void process(IncomingMessageEnvelope envelope,
                        MessageCollector collector,
                        TaskCoordinator coordinator) {
        String message   = (String) envelope.getMessage();
        String partition = (String) envelope.getKey();

        //System.out.println("MESSAGE:" + message);
        String output = pipeline(message);
        collector.send(new OutgoingMessageEnvelope(OUTPUT_STREAM, partition, output));

    }
}
