package no.nav.helse.e2e

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.producer.ProducerRecord

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class SykePengeBehandlingE2E {

    private val log = LoggerFactory.getLogger(SykePengeBehandlingE2E::class.java.name)
    val kafkaClient = KafkaClient()
    val producer = kafkaClient.producer
    val consumer = kafkaClient.consumer
    val aktorId : String
    val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    init {
        val (_, _, result) = "http://localhost:8060/api/testscenario/40".httpPost().responseString()
        val json = defaultObjectMapper.readValue(result.get(), JsonNode::class.java)
        aktorId = json.get("personopplysninger").get("søkerAktørIdent").asText()
        log.info("Created scenario for person with id $aktorId")
    }

    @Test
    fun e2e_what_goes_in_must_come_out () {
        log.info("sending a message")
        consumer.subscribe(listOf(Topics.VEDTAK_SYKEPENGER.name))
        val soknad = createSoknad()
        producer.send(ProducerRecord(Topics.SYKEPENGESØKNADER_INN.name,defaultObjectMapper.writeValueAsString(soknad)))
        producer.flush()
        val records = consumer.poll(Duration.ofSeconds(10))
        consumer.commitSync()
        log.info(records.first().value())
        assertEquals(1,records.count())
    }

    fun createSoknad(): Sykepengesoknad {
        return Sykepengesoknad(aktorId, Arbeidsgiver("Firma 1", "979191138"), false,
                LocalDate.of(2018,3,1), LocalDate.of(2018,4,1),
                LocalDate.of(2018,3,1), null, emptyList(), false)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesoknad(val aktorId: String,
                           val arbeidsgiver: Arbeidsgiver,
                           val soktUtenlandsopphold: Boolean,
                           val fom: LocalDate,
                           val tom: LocalDate,
                           val startSyketilfelle: LocalDate,
                           val sendtNav: LocalDateTime?,
                           val soknadsperioder: List<Soknadsperiode>,
                           val harVurdertInntekt: Boolean)


data class Arbeidsgiver(val navn : String , val orgnummer : String )

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)
