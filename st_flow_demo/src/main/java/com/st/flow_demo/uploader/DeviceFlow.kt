package com.st.flow_demo.uploader
import com.st.flow_demo.helpers.extractAllSensorsFromCompositeFlow
import com.st.flow_demo.helpers.getCompositeInputFlowCount
import com.st.flow_demo.helpers.serializeForSendingFlows
import com.st.flow_demo.helpers.serializeForSendingIfFlows
import com.st.flow_demo.models.Flow
import com.st.blue_sdk.board_catalog.models.Sensor
import com.st.flow_demo.serializer.BoardFlowSerializer
import com.st.flow_demo.serializer.BoardSensorSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.HashMap

@Serializable
data class DeviceFlow(
    @SerialName(value = "flows")
    var flows: MutableList<@Serializable(with = BoardFlowSerializer::class) Flow> = ArrayList(),
    @SerialName(value = "sensors")
    var sensors: MutableList<@Serializable(with = BoardSensorSerializer::class) Sensor> = ArrayList(),
    @SerialName(value = "version")
    var version: Int = 0
) {

    companion object {
        fun getBoardStream(flows: List<Flow>): String {
            val deviceFlows = mutableListOf<DeviceFlow>()
            flows.forEach { deviceFlows.add(transform(it)) }
            val retValue = serializeForSendingFlows(deviceFlows.toList())

            //Log.i("FlowTmp", "Flow getBoardStream =$retValue")

            return retValue
        }

        fun getBoardStream(exp: Flow, stats: List<Flow>): String {

            //Log.i("FlowTmp", "Flow exp =$exp")

            val expression = transform(exp)

            //Log.i("FlowTmp", "Flow expression =$expression")

            //Log.i("FlowTmp", "Flow stats =$stats")

            val statements = mutableListOf<DeviceFlow>()
            stats.forEach { statements.add(transform(it)) }

            //Log.i("FlowTmp", "Flow statements =$statements")

            val retValue = serializeForSendingIfFlows(DeviceIfStatementFlow(expression = expression, statements = statements.toList()))

            //Log.i("FlowTmp", "Flow getBoardStream =$retValue")

            return retValue
        }

        private fun transform(flow: Flow): DeviceFlow {
            val out = DeviceFlow()
            out.version = flow.version
            extractAllSensorsFromCompositeFlow(flow, out.sensors)
            extractAllFlowsFromCompositeFlow(flow, out.flows)
            out.apply {
                mergeSensorConfigs()
                orderSensors()
                orderFlows()
            }
            return out
        }

        private fun extractAllFlowsFromCompositeFlow(flow: Flow, flows: MutableList<Flow>) {
            flows.add(flow)
            for (parent in flow.flows) {
                extractAllFlowsFromCompositeFlow(parent, flows)
            }
        }

    }

    private fun mergeSensorConfigs() {

        val mergedMap = HashMap<String, Sensor>()
        for (sensor in sensors) {
            if (!mergedMap.containsKey(sensor.id)) {
                mergedMap[sensor.id] = sensor
            }
        }

        sensors = ArrayList(mergedMap.values)
    }

    private fun orderSensors() {

        sensors.sortWith(Comparator { sensor1, sensor2 -> sensor1.id.compareTo(sensor2.id) })
    }

    private fun orderFlows() {
        flows.sortWith(Comparator { flow1, flow2 ->
            getCompositeInputFlowCount(flow2) - getCompositeInputFlowCount(
                flow1
            )
        })
    }
}