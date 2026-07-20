package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.annotation.JsonValue
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
}

data class Produsent(
    val cluster: String,
    val namespace: String,
    val appnavn: String,
)

data class EventHistorikkRad(
    val hendelsesId: String,
    val statuskortId: String,
    val ident: String,
    val eventType: String,
    val data: String?,
    val konsumert: ZonedDateTime,
)
