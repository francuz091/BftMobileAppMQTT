package hr.algebra.bftwebappmqtt

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import hr.algebra.bftwebappmqtt.databinding.ActivityMainBinding

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck

import java.nio.charset.StandardCharsets
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mqttHost = "0129e7865c6e4b78b216ad110505c749.s1.eu.hivemq.cloud"
    private val mqttPort = 8883
    private val mqttUsername = "esp32"
    private val mqttPassword = "123456789"
    private val commandTopic = "esp32/test/command"
    private val statusTopic = "esp32/test/status"
    private lateinit var mqttClient: Mqtt3AsyncClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Kreiranje MQTT clienta
        createMqttClient()
        connectToHiveMQ()



        binding.btnPin1.setOnClickListener { sendCommand("PIN1") }
        binding.btnPin2.setOnClickListener { sendCommand("PIN2") }
        binding.btnPin3.setOnClickListener { sendCommand("PIN3") }
        binding.btnPin4.setOnClickListener { sendCommand("PIN4") }
    }

    private fun createMqttClient() {

        val clientId =
            "Android-" + UUID.randomUUID().toString()


        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(mqttHost)
            .serverPort(mqttPort)
            .useSslWithDefaultConfig()
            .buildAsync()

        // Primanje MQTT poruka

        mqttClient.publishes(
            MqttGlobalPublishFilter.ALL
        ) { publish ->

            handleIncomingMessage(publish)

        }
    }

    private fun connectToHiveMQ() {

        updateStatus(
            "Spajanje na HiveMQ..."
        )

        mqttClient.connectWith()
            .simpleAuth()
            .username(mqttUsername)
            .password(
                mqttPassword.toByteArray(
                    StandardCharsets.UTF_8
                )
            )
            .applySimpleAuth()
            .send()
            .whenComplete {
                    _: Mqtt3ConnAck?,
                    throwable: Throwable? ->

                runOnUiThread {

                    if (throwable != null) {

                        updateStatus(
                            "GREŠKA: ${throwable.message}"
                        )

                    } else {

                        updateStatus(
                            "CONNECTED"
                        )

                        subscribeToStatus()

                    }
                }
            }
    }


    private fun subscribeToStatus() {

        mqttClient.subscribeWith()
            .topicFilter(statusTopic)
            .qos(
                MqttQos.AT_LEAST_ONCE
            )
            .send()
            .whenComplete {
                    _,
                    throwable ->

                runOnUiThread {

                    if (throwable != null) {

                        addMessage(
                            "Subscribe greška: ${throwable.message}"
                        )

                    } else {

                        addMessage(
                            "Connected to $statusTopic"
                        )

                    }
                }
            }
    }


    private fun sendCommand(
        command: String
    ) {

        if (!mqttClient.state.isConnected) {

            updateStatus(
                "Nije spojen na HiveMQ!"
            )

            return
        }

        mqttClient.publishWith()
            .topic(commandTopic)
            .qos(
                MqttQos.AT_LEAST_ONCE
            )
            .payload(
                command.toByteArray(
                    StandardCharsets.UTF_8
                )
            )
            .send()
            .whenComplete {
                    _,
                    throwable ->

                runOnUiThread {

                    if (throwable != null) {

                        addMessage(
                            "GREŠKA: ${throwable.message}"
                        )

                    } else {

                        addMessage(
                            "→ ESP32: $command"
                        )

                    }
                }
            }
    }


    private fun handleIncomingMessage(
        publish: Mqtt3Publish
    ) {

        val topic =
            publish.topic.toString()

        val message =
            String(
                publish.payloadAsBytes,
                StandardCharsets.UTF_8
            )

        runOnUiThread {

            addMessage(
                "← $topic\n$message"
            )

        }
    }


    private fun disconnectFromHiveMQ() {

        if (!mqttClient.state.isConnected) {

            updateStatus(
                "Već je disconnected."
            )

            return
        }

        mqttClient.disconnect()
            .whenComplete {
                    _,
                    throwable ->

                runOnUiThread {

                    if (throwable != null) {

                        updateStatus(
                            "Disconnect greška"
                        )

                    } else {

                        updateStatus(
                            "DISCONNECTED"
                        )

                    }
                }
            }
    }

    private fun updateStatus(
        status: String
    ) {
        binding.tvConnectionStatus.text =
            "Status: $status"
    }


    private fun addMessage(
        message: String
    ) {
//        val oldText =
//            binding.tvMessages.text.toString()
//
//        if (oldText == "Nema poruka.") {
//
//            binding.tvMessages.text =
//                message
//        } else {
//            binding.tvMessages.text =
//                "$oldText\n\n$message"
//        }
    }


    override fun onDestroy() {

        if (::mqttClient.isInitialized) {
            if (mqttClient.state.isConnected) {
                mqttClient.disconnect()
            }
        }
        super.onDestroy()
    }
}