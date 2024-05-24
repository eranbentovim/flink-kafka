package examples.kafka

import examples.kafka.data.getInMemorySensorData
import examples.kafka.model.SensorData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.connector.source.Source
import org.apache.flink.connector.base.DeliveryGuarantee
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.formats.json.JsonDeserializationSchema
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

class StreamingJob {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val env = StreamExecutionEnvironment.getExecutionEnvironment()

//            val source: KafkaSource<SensorData> = KafkaSource
//                .builder<SensorData>()
//                .setBootstrapServers("localhost:9092")
//                .setTopics("input-topic")
//                .setGroupId("test")
//                .setValueOnlyDeserializer(JsonDeserializationSchema(SensorData::class.java))
//                .build()

            val sink = KafkaSink.builder<String>()
                .setBootstrapServers("localhost:9092")
                .setRecordSerializer(
                    KafkaRecordSerializationSchema.builder<String>()
                        .setTopic("output-topic")
                        .setValueSerializationSchema(SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build()
            //test sink
            env.fromElements("elm1", "elm2").sinkTo(sink)


            val stream: DataStream<SensorData> =
                //env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                env.fromCollection(getInMemorySensorData()).name("in-memory-input")
//            stream.keyBy(SensorData::sensorId)
//                .filter { it.status == "OK" && it.value > 0.0 }
//                .map { Json.encodeToString(it) }
//                .sinkTo(sink)

            env.execute("Flink Kafka Example")
        }
    }
}
