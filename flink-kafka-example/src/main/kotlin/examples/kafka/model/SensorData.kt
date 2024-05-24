package examples.kafka.model

import com.lapanthere.flink.api.kotlin.typeutils.DataClassTypeInfoFactory
import kotlinx.serialization.Serializable
import org.apache.flink.api.common.typeinfo.TypeInfo

@TypeInfo(DataClassTypeInfoFactory::class)
@Serializable
data class SensorData(
    val sensorId: String,
    val value: Double,
    val status: String,
//    val timestamp: Long? = null,
//    val unit: String? = null,
//    val location: Location? = null,
//    val sensorType: String? = null,
)

@TypeInfo(DataClassTypeInfoFactory::class)
data class Location(
    val latitude: Double,
    val longitude: Double
)