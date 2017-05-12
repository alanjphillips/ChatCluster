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

Or with client microservice to send to Kafka:

> docker-compose -f docker-compose-with-client.yml up -d --no-recreate

Scale up some nodes:

> docker-compose scale cassandra-node=2 chatservice-node=2

4) Connect to bash shell on kafka-1 host, then run:

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic instant_message_in --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic instant_message_out --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic latest_messages_request --partitions 3 --replication-factor 3

> kafka-topics.sh --zookeeper zookeeper:2181 --create --topic latest_messages_block --partitions 3 --replication-factor 3

This will create 3 Topic partitions that are spread amongst the 3 Kafka nodes. Each partition leader will have 2 replicas