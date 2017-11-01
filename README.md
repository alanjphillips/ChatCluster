Chat Cluster
============================================
Chat Cluster. Horizontally scaled out use of Akka Cluster Sharding with Cassandra Cluster configured for persistence. Messages are consumed from Kafka and persisted to Cassandra using akka-persistence

Install docker, docker-machine and docker-compose. See docker docs on how to create machine in virtualbox
https://docs.docker.com/machine/drivers/virtualbox/

Create a VM called 'default'

> docker-machine create --driver virtualbox default

Or for a beefier machine

> docker-machine create --driver virtualbox --virtualbox-memory "8192" --virtualbox-cpu-count "2" default

Start up 'default' machine

> docker-machine start default

1) Connect to 'default' machine

> eval "$(docker-machine env default)"

2) CD into project and use SBT to build and publish to local Docker repo:

> sbt clean docker:publishLocal

3) Run docker compose to launch Chat Cluster and Cassandra which is used for persistence

> docker-compose up -d --no-recreate

Scale up some nodes:

> docker-compose scale cassandra-node=2 chatservice-node=2

4) Connect to bash shell on kafka-1 host, then run:

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic instant_message_in --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic instant_message_out --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic latest_messages_request --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic latest_messages_block --partitions 3 --replication-factor 3

This will create 3 Topic partitions that are spread amongst the 3 Kafka nodes. Each partition leader will have 2 replicas

5) Send a chat message to Kafka partition 0 of `instant_message_in` topic using Kafka Tool or Cmd-line kafka producer: 

```json
{
  "conversationKey": "abc123", 
  "sender": "bob", 
  "recipients": ["ann"], 
  "body": "Hi there!"
}
```

6) Then check out the Kafka partitions of `instant_message_out` topic, the following event should be there:

```json
{
  "conversationKey": "abc123",
  "sender": "bob",
  "recipients": [
    "ann"
  ],
  "conversationMsgSeq": 0,
  "body": "Hi there!"
}
```

7) Send a request for latest chat messages in a conversation to Kafka partition 0 of `latest_messages_request` topic:

```json
{
  "conversationKey": "abc123",  
  "numMsgs": 100
}
```

8) Then checkout the Kafka partitions of `latest_messages_block` topic, the following event should be there:
- an extra chat message that was sent in response is included below

```json
{
  "conversationKey": "abc123",
  "latestChatter": [
    [
      "bob",
      "Hi there!",
      0
    ],
    [
      "ann",
      "Hi there!",
      1
    ]
  ]
}
```

Kafka Notes
===========
- Kafka Consumer Groups
Kafka Consumers label themselves with a consumer group name, and each record published to a topic is delivered to one consumer instance within each subscribing consumer group. Consumer instances can be in separate processes or on separate machines.
-> If all the consumer instances have the same consumer group, then the records will effectively be load balanced over the consumer instances.
-> If all the consumer instances have different consumer groups, then each record will be broadcast to all the consumer processes.

- According to Kafka docs, a partition can have just one consumer:
"[a] topic is divided into a set of totally ordered partitions, each of which is consumed by exactly one consumer within each subscribing consumer group at any given time"

- NB: From Kafka: The Definite Guide: "If we add more consumers to a single group with a single topic than we have partitions, some of the consumers will be idle and get no messages at all".

- Consumers in a consumer group share ownership of the partitions in the topics they subscribe to. When we add a new consumer to the group, it starts consuming messages from partitions previously consumed by another consumer. The same thing happens when a consumer shuts down or crashes; it leaves the group, and the partitions it used to consume will be consumed by one of the remaining consumers. Reassignment of partitions to consumers also happen when the topics the consumer group is consuming are modified (e.g., if an administrator adds new partitions).

- Aside: To achieve in-order processing of messages from a partition there is 1 consumer per partition. A topic can consist of one or more partitions, amongst which messages are distributed by a (partition) key. Remember: Kafka only provides a total order over messages within a partition, not between different partitions in a topic.
Whenever the consumer calls poll(), it returns records written to Kafka that consumers in our group have not read yet.

- Load balancing in Kafka
Kafka achieves load balancing by distributing messaging across multiple partitions in a topic. The producer is responsible for choosing which partition to send a message to, for example key.hashCode() % numberOfPartitions
This allows multiple consumers to pull data from these partitions
