# Spring Boot Microservices Ecommerce Demo

A simple microservices application for experimenting and demonstrating the microservices architecture using Spring Boot.

## Shared API responses

`shared/common` is a regular JAR containing response envelopes, validation error
details, and `ApiResponseUtil` in `com.microservices.common.response`. Each service
keeps its own exception handler, mapper, and domain exceptions. The library has no
Spring components and requires no component scanning or imports to register beans.

Add this dependency to services that need the shared response format:

```xml
<dependency>
    <groupId>com.microservices</groupId>
    <artifactId>common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

From the repository root, build customer-service and its shared dependency:

```sh
mvn -pl services/customer-service -am verify
```

Install the library locally before building or running a service independently:

```sh
mvn -pl shared/common install
```

Rebuild consuming services after changing shared code. For new services, add their
directory to the root POM's modules. Pass the HTTP status explicitly to `ApiResponseUtil.success(status, data, message)`
and `ApiResponseUtil.error(status, message)`, and set the same HTTP status on the
controller's `ResponseEntity`. For example, use `HttpStatus.OK` for retrieval and
`HttpStatus.CREATED` for creation.

## Local Kafka (KRaft)

Docker Compose includes `apache/kafka:4.2.2` as `ms_kafka`, using one combined
broker/controller. The broker stores and serves messages; the KRaft controller
manages cluster metadata. ZooKeeper is not needed. This follows the tutorial's
single-broker approach, with Kafka's own controller replacing ZooKeeper.

Start Docker Desktop, then start only Kafka:

```sh
docker compose up -d --wait kafka
docker compose ps kafka
```

Applications running from your IDE connect to `localhost:9092`. When you reach
Kafka integration in the tutorial, configure the relevant Spring service with:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

Applications running on the same Compose network connect to `kafka:19092` instead.
Compose provides that network automatically. `listeners` defines where Kafka
accepts connections; `advertised.listeners` defines the addresses Kafka gives
clients for subsequent requests. A container's `localhost` refers to itself,
which is why the Docker address is different from the host address.

The controller uses `kafka:29093` internally; its port is not published to your
Mac. `KAFKA_NODE_ID: 1` identifies the node, and `1@kafka:29093` identifies the
single controller voter. `CLUSTER_ID` identifies this local cluster and should
stay unchanged while its volume is reused. The `kafka` volume stores messages and
metadata across container restarts.

Replication factors and minimum in-sync replica settings are `1` because only
one broker exists. The consumer-group initial rebalance delay is `0` for quicker
local startup. The health check lists topics through the broker API. This setup
uses plaintext connections and one node for local learning; it has no redundancy.

Try a message round trip after Kafka is healthy:

```sh
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:19092 --create --if-not-exists \
  --topic kafka-smoke-test --partitions 1 --replication-factor 1

printf 'hello kafka\n' | docker compose exec -T kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka:19092 --topic kafka-smoke-test

docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:19092 --topic kafka-smoke-test \
  --from-beginning --max-messages 1 --timeout-ms 10000
```

The consumer should print `hello kafka`. For troubleshooting, run
`docker compose logs --tail=100 kafka`. To stop just Kafka while retaining its
data, run `docker compose stop kafka`.

This adds the Kafka infrastructure. Order events, payment events, serializers,
and notification consumers will be added as you reach those tutorial sections.

References: [Apache's Docker guide](https://kafka.apache.org/42/getting-started/docker/)
and [single-node Compose example](https://github.com/apache/kafka/blob/trunk/docker/examples/docker-compose-files/single-node/plaintext/docker-compose.yml).
