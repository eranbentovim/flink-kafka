package examples.kafka.data

import examples.kafka.model.SensorData

fun getInMemorySensorData(): List<SensorData> {
    return listOf(
        SensorData(sensorId = "1234", value = 1.0, status = "OK"),
        SensorData(sensorId = "12345", value = 1.0, status = "FAIL")
    )
}