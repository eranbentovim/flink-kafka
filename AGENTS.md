# AGENTS.md

Guidance for coding agents working in this repository.

## What this repo is

A local, `docker compose`-based playground for a 2-node Kafka (KRaft) cluster with
observability (Prometheus, Grafana, Kafka UI, JMX + Kafka exporters), plus a small
Apache Flink streaming job written in Kotlin that reads from / writes to Kafka.

`README.md` at the root is a tutorial-style write-up of how the compose stack was
built up piece by piece. Treat it as documentation that must be kept in sync when
you change `docker-compose.yml` or any config under `prometheus/`, `grafana/`,
`kafka-ui/` or `jmx-exporter/`.

## Layout

| Path | What it holds |
| --- | --- |
| `docker-compose.yml` | The whole stack: `kafka-0`, `kafka-1`, `kafka-ui`, `prometheus`, `kafka-exporter`, `grafana`, `jobmanager`, `taskmanager` |
| `flink-kafka-example/` | Maven + Kotlin Flink job (`examples.kafka.StreamingJob`) |
| `kafka-ui/config.yml` | Kafka UI clusters + auth (admin/admin) |
| `prometheus/prometheus.yml` | Scrape config for `kafka-exporter:9308` and `kafka-{0,1}:9404` |
| `grafana/provisioning/`, `grafana/dashboards/` | Datasource + dashboard provisioning (Strimzi dashboards) |
| `jmx-exporter/` | The JMX Prometheus javaagent jar + its Kafka config, bind-mounted into both brokers |

Note: `flink-kafka-example/docker-compose.yml` is a leftover standalone Flink-only
stack. The root `docker-compose.yml` is the one to use — it wires Flink onto the
same `kafka` network as the brokers.

## Running the stack

```shell
docker compose up            # everything
docker compose up -d kafka-0 kafka-1   # brokers only
docker compose down -v       # tear down including volumes
```

Endpoints once healthy:

- Kafka UI — http://localhost:8080 (`admin` / `admin`, see `kafka-ui/config.yml`)
- Grafana — http://localhost:3000 (`admin` / `admin`, see `docker-compose.yml`)
- Prometheus — http://localhost:9090
- Flink UI — http://localhost:8081
- Kafka exporter metrics — http://localhost:9308/metrics

Brokers are reachable **inside** the compose network as `kafka-0:9092,kafka-1:9092`.
`9092` is exposed without a fixed host port, so from the host use
`docker compose port kafka-0 9092` to find the mapped port rather than assuming
`localhost:9092`.

## Kafka CLI cheatsheet

```shell
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-0:9092,kafka-1:9092 --list

docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server kafka-0:9092,kafka-1:9092 --replication-factor 1 --partitions 1 --topic test

docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka-0:9092,kafka-1:9092 --topic test --from-beginning
```

## Building and running the Flink job

```shell
cd flink-kafka-example
mvn clean package          # shaded jar in target/, main class examples.kafka.StreamingJob
mvn test                   # JUnit 5 via surefire (no tests exist yet)
```

Submit the shaded jar either through the Flink UI (http://localhost:8081, the
job manager sets `web.upload.dir=/opt/flink/flink-web`) or by dropping it into
`./flink/data/usrlib`, which is bind-mounted into both `jobmanager` and
`taskmanager` at `/opt/flink/usrlib`.

The `./flink/data/*` directories are not committed; Docker creates them on first
`up`, so they may end up root-owned. Create them yourself first if that causes
permission trouble.

## Conventions and gotchas

- **Kotlin, not Java.** Sources live under `src/main/kotlin`; `pom.xml` overrides
  `sourceDirectory` accordingly. JVM target is 1.8, Kotlin 1.9.24, Flink 1.17.1.
- **Flink type information.** Data classes carried through a `DataStream` are
  annotated `@TypeInfo(DataClassTypeInfoFactory::class)` (from `com.lapanthere:flink-kotlin`)
  so Flink does not fall back to Kryo. Add that annotation to any new data class
  that flows through the pipeline, and `@Serializable` if it is JSON-encoded via
  `kotlinx.serialization`.
- **Bootstrap servers in job code.** `StreamingJob.kt` currently hardcodes
  `localhost:9092`. When the job runs inside the compose stack this must be
  `kafka-0:9092,kafka-1:9092` — prefer making it a parameter over swapping the
  hardcoded string.
- **Commented-out code is intentional scaffolding.** `StreamingJob.kt` keeps the
  Kafka source and the keyBy/filter/map pipeline commented out and runs off
  `getInMemorySensorData()` instead. Don't delete those blocks as "dead code"
  without being asked — they are the next step of the example.
- **Version bumps ripple.** `flink.version` is shared by `flink-streaming-java`,
  `flink-connector-kafka`, `flink-json` and `flink-clients`; the Flink Docker image
  is `flink:latest`, so a large version gap between the two will break job submission.
- **The JMX agent path is pinned.** `EXTRA_ARGS` in `docker-compose.yml` references
  `jmx_prometheus_javaagent-0.19.0.jar` by exact filename. Replacing the jar in
  `jmx-exporter/` means updating that string too, or the brokers will fail to start.
- **Metrics wiring is duplicated.** A new scrape target generally needs edits in both
  `prometheus/prometheus.yml` and, if it should show up in Kafka UI, `kafka-ui/config.yml`.
- Credentials in this repo are throwaway local defaults. Don't introduce real secrets;
  keep anything sensitive out of committed config.

## Before you finish

- `docker compose config` to validate compose edits.
- `mvn clean package` from `flink-kafka-example/` for any Kotlin change.
- If you changed the stack's shape, update the corresponding section of `README.md`.
