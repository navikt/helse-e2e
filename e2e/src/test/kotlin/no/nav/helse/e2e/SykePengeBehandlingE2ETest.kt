package no.nav.helse.e2e

import org.apache.kafka.clients.producer.ProducerRecord

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration

class SykePengeBehandlingE2ETest {

    private val LOG = LoggerFactory.getLogger(SykePengeBehandlingE2ETest::class.java.name)
    val kafkaClient = KafkaClient()
    val producer = kafkaClient.producer
    val consumer = kafkaClient.consumer

    //@Test
    fun e2e_what_goes_in_must_come_out () {
        LOG.info("sending a message")
        consumer.subscribe(listOf("aapen-helse-sykepenger-vedtak"))
        val readText = SykePengeBehandlingE2ETest::class.java.getResource("/test_soknad.json").readText()
        producer.send(ProducerRecord("privat-sykepengebehandling",readText))
        val records = consumer.poll(Duration.ofSeconds(10))
        consumer.commitSync()
        assertEquals(1,records.count())
    }

}