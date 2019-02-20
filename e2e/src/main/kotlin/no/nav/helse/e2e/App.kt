package no.nav.helse.e2e

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList

private val KAFKA_BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS")
private val LOG = LoggerFactory.getLogger("e2e")
private val admin = AdminClient.create(Properties().apply {
    put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS) })

fun main() {
    createKafkaTopics()
    LOG.info("Starting e2e server")
    embeddedServer(Netty, 3231) {
        routing {
            get("/ruok") {
                call.respond("imok")
            }
        }
    }.start(wait = false)
}

fun createKafkaTopics() {
    createTopic(Topics.SYKEPENGESØKNADER_INN.name)
    createTopic(Topics.VEDTAK_SYKEPENGER.name)
}

private fun createTopic(topicName: String) {
    if (!admin.listTopics().names().get().contains(topicName)) {
        LOG.info("creating topic: {}", topicName)
        val topicList = ArrayList<NewTopic>()
        val newTopic = NewTopic(topicName, 3, 1)
        topicList.add(newTopic)
        admin.createTopics(topicList)
    }
}

