package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import org.postgresql.util.PGobject
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun defaultObjectMapper(): ObjectMapper = jacksonMapperBuilder()
    .addModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)

fun nowAtUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)

fun Any?.toJsonb(objectMapper: ObjectMapper = defaultObjectMapper()): PGobject? =
    this?.let {
        PGobject().apply {
            type = "jsonb"
            value = objectMapper.writeValueAsString(it)
        }
    }

inline fun <reified T> Row.json(label: String, objectMapper: ObjectMapper): T =
    objectMapper.readValue(string(label))

fun parseSensitivitet(value: String): Sensitivitet =
    Sensitivitet.entries.firstOrNull { it.name.lowercase() == value.lowercase() }
        ?: throw IllegalArgumentException("Kunne ikke tolke sensitivitet: $value")
