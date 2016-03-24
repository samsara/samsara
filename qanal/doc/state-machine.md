# Qanal state machine


These are the possible errors:

| Error Code | Description                   | Action                            |
|-----------:|-------------------------------|-----------------------------------|
|          1 | Cannot talk to Kafka          | Retry                             |
|          2 | Topic doesnt exist            | Retry                             |
|          3 | Partition doesnt exist        | Retry                             |
|          4 | Message too big               | Send to Error                     |
|          5 | Offset doesnt exist           | Action - Reset to earliest/latest |
|          6 | Network exceptions            | Retry                             |
|          7 | Message format Invalid        | Send to Error                     |
|          8 | Invalid Event                 | Send to Error                     |
|          9 | Transformation Error          | Send to Error                     |
|         10 | Cannot talk to ES             | Retry                             |
|         11 | ES Error (5xx)                | Retry                             |
|         12 | Bulk response contains errors | Send to Error                     |
|         13 | ES Error (4xx)                | Retry                             |


This is the state machine used for processing

![State Machine](./images/state-machine.png)
