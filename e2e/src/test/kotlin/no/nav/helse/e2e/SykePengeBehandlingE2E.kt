package no.nav.helse.e2e

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.ProducerRecord

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.streams.defaultObjectMapper

class SykePengeBehandlingE2E {

    private val log = LoggerFactory.getLogger(SykePengeBehandlingE2E::class.java.name)
    val kafkaClient = KafkaClient()
    val producer = kafkaClient.producer
    val consumer = kafkaClient.consumer

    init {
        consumer.subscribe(listOf(Topics.VEDTAK_SYKEPENGER.name, Topics.SYKEPENGEBEHANDLINGSFEIL.name))
    }

    @Test
    fun `e2e scenario 200 person med for lav inntekt`  () {
        val (_, _, result) = "http://localhost:8060/api/testscenario/200".httpPost().responseString()
        val soknad = sykepengesoknad(result)
        val records = sendSoknad(soknad)
        assertEquals(Topics.SYKEPENGEBEHANDLINGSFEIL.name, records!!.first().topic())
        Thread.sleep(1000)
    }

    @Test
    fun `e2e scenario 201 person som tilfredstille alle kravene` () {
        val (_, _, result) = "http://localhost:8060/api/testscenario/201".httpPost().responseString()
        val soknad = sykepengesoknad(result)
        val records = sendSoknad(soknad)
        assertEquals(Topics.VEDTAK_SYKEPENGER.name, records!!.first().topic())
        Thread.sleep(1000)
    }

    @Test
    fun `e2e scenario 202 person med 2 arbeidsforhold` () {
        val (_, _, result) = "http://localhost:8060/api/testscenario/202".httpPost().responseString()
        val soknad = sykepengesoknad(result)
        val records = sendSoknad(soknad)
        assertEquals(Topics.SYKEPENGEBEHANDLINGSFEIL.name, records!!.first().topic())
        Thread.sleep(1000)
    }

    @Test
    fun `e2e scenario 203 person som er for gammel`() {
        val (_, _, result) = "http://localhost:8060/api/testscenario/203".httpPost().responseString()
        val soknad = sykepengesoknad(result)
        val records = sendSoknad(soknad)
        assertEquals(Topics.SYKEPENGEBEHANDLINGSFEIL.name, records!!.first().topic())
        Thread.sleep(1000)
    }

    private fun sendSoknad(soknad: Sykepengesoknad): ConsumerRecords<String, String>? {
        producer.send(ProducerRecord(Topics.SYKEPENGESØKNADER_INN.name, defaultObjectMapper.writeValueAsString(soknad)))
        producer.flush()
        val records = consumer.poll(Duration.ofSeconds(30))
        consumer.commitSync()
        val json = defaultObjectMapper.readTree(records.first().value())
        log.info(defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json))
        return records
    }

    private fun sykepengesoknad(result: Result<String, FuelError>): Sykepengesoknad {
        val json = defaultObjectMapper.readValue(result.get(), JsonNode::class.java)
        val aktorId = json.get("personopplysninger").get("søkerAktørIdent").asText()
        val orgnummer = json.get("scenariodata").get("aareg").get("arbeidsforhold").get(0).get("arbeidsgiverOrgnr").asText()
        log.info("Created scenario for person with id $aktorId")
        log.info("sender søknad")
        val soknad = createSoknad(aktorId, orgnummer)
        println(defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(soknad))
        return soknad
    }

    fun createSoknad(aktorId: String, orgnummer: String): Sykepengesoknad {
        return Sykepengesoknad(aktorId, Arbeidsgiver("AS MOCK", orgnummer), false,
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1),
                LocalDate.now().minusMonths(2), LocalDateTime.now(),
                listOf(Soknadsperiode(LocalDate.now().minusMonths(2),LocalDate.now().minusMonths(1), 100)), false)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesoknad(val aktorId: String,
                           val arbeidsgiver: Arbeidsgiver,
                           val soktUtenlandsopphold: Boolean,
                           val fom: LocalDate,
                           val tom: LocalDate,
                           val startSyketilfelle: LocalDate,
                           val sendtNav: LocalDateTime = LocalDateTime.now(),
                           val soknadsperioder: List<Soknadsperiode>,
                           val harVurdertInntekt: Boolean,
                           val status: String = "SENDT")


data class Arbeidsgiver(val navn : String , val orgnummer : String )

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)
