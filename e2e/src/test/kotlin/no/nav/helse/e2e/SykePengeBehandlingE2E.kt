package no.nav.helse.e2e

import no.nav.helse.streams.Topics
import org.apache.kafka.clients.producer.ProducerRecord

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration

class SykePengeBehandlingE2E {

    private val LOG = LoggerFactory.getLogger(SykePengeBehandlingE2E::class.java.name)
    val kafkaClient = KafkaClient()
    val producer = kafkaClient.producer
    val consumer = kafkaClient.consumer

    @Test
    fun e2e_what_goes_in_must_come_out () {
        LOG.info("sending a message")
        consumer.subscribe(listOf(Topics.VEDTAK_SYKEPENGER.name))
        val readText = SykePengeBehandlingE2E::class.java.getResource("/test_soknad.json").readText()
        producer.send(ProducerRecord(Topics.SYKEPENGESØKNADER_INN.name,readText))
        producer.flush()
        val records = consumer.poll(Duration.ofSeconds(10))
        consumer.commitSync()
        assertEquals(1,records.count())
    }

}