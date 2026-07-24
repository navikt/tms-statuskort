package no.nav.tms.statuskort

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

data class Statuskort(
    val statuskortId: String,
    val ident: String,
    val tjeneste: String,
    val innhold: Innhold,
    val sensitivitet: Sensitivitet,
    val produsent: Produsent,
    val aktiv: Boolean,
    val opprettet: ZonedDateTime,
    val sistEndret: ZonedDateTime,
    val inaktivert: ZonedDateTime?,
)

data class Innhold(
    val bokmaal: Tekstinnhold,
    val nynorsk: Tekstinnhold,
    val engelsk: Tekstinnhold,
)

data class Tekstinnhold(
    val link: String,
    val tittel: String,
    val beskrivelse: String,
)

enum class Sensitivitet {
    Substantial,
    High;

    @JsonValue
    fun toJson() = name.lowercase()

    companion object {
        fun parse(value: String): Sensitivitet {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Kunne ikke tolke sensitivitet: $value")
        }
    }
}

data class Produsent(
    val cluster: String,
    val namespace: String,
    val appnavn: String,
)

data class EventHistorikkEntry(
    val statuskortId: String,
    val eventType: String,
    val innhold: JsonNode?,
    val tidspunkt: ZonedDateTime,
)
