package no.nav.tms.statuskort.action

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

const val StatuskortActionVersion = "1.0.0"

enum class EventType {
    Opprett, Oppdater, Inaktiver;

    @JsonValue
    fun toJson() = name.lowercase()
}

data class OpprettStatuskort(
    val statuskortId: String,
    val ident: String,
    val tjeneste: String,
    val innhold: Innhold,
    val sensitivitet: Sensitivitet,
    val produsent: Produsent,
    val metadata: Map<String, Any>?
) {
    @JsonProperty("@event_name") val eventName = EventType.Opprett
}

data class OppdaterStatuskort(
    val statuskortId: String,
    val innhold: Innhold,
    val metadata: Map<String, Any>?
) {
    @JsonProperty("@event_name") val eventName = EventType.Oppdater
}

data class InaktiverStatuskort(
    val statuskortId: String,
    val metadata: Map<String, Any>?
) {
    @JsonProperty("@event_name") val eventName = EventType.Inaktiver
}

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

class StatuskortValidationException(message: String) : IllegalArgumentException(message)
