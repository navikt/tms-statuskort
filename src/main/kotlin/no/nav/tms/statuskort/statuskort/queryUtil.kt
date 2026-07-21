package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import kotliquery.Row
import no.nav.tms.common.postgres.JsonbHelper.json
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun defaultObjectMapper(): ObjectMapper = jacksonMapperBuilder()
    .addModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()
    .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

object ZonedDateTimeHelper {
    fun nowAtUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
}

fun parseSensitivitet(value: String): Sensitivitet =
    Sensitivitet.entries.firstOrNull { it.name.lowercase() == value.lowercase() }
        ?: throw IllegalArgumentException("Kunne ikke tolke sensitivitet: $value")

fun Row.tilStatuskort(): Statuskort = Statuskort(
    statuskortId = string("statuskortId"),
    ident = string("ident"),
    tjeneste = string("tjeneste"),
    innhold = json("innhold"),
    sensitivitet = parseSensitivitet(string("sensitivitet")),
    produsent = json("produsent"),
    aktiv = boolean("aktiv"),
    inaktivert = zonedDateTimeOrNull("inaktivert"),
    opprettet = zonedDateTime("opprettet"),
    sistEndret = zonedDateTime("sistEndret"),
)
