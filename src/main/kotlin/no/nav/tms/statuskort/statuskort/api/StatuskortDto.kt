package no.nav.tms.statuskort.statuskort.api

import no.nav.tms.statuskort.statuskort.Innhold
import no.nav.tms.statuskort.statuskort.Statuskort
import no.nav.tms.statuskort.statuskort.Tekstinnhold

data class StatuskortResponse(
    val statuskort: List<StatuskortDto>,
    val harSkjulteKort: Boolean,
)

data class StatuskortDto(
    val id: String,
    val tjeneste: String,
    val innhold: TekstinnholdDto,
)

data class TekstinnholdDto(
    val tittel: String,
    val beskrivelse: String,
    val link: String,
)

enum class ApiLocale(val kode: String) {
    NB("nb"),
    NN("nn"),
    EN("en");

    fun velgTekst(innhold: Innhold): Tekstinnhold = when (this) {
        NB -> innhold.bokmaal
        NN -> innhold.nynorsk
        EN -> innhold.engelsk
    }

    companion object {
        const val DEFAULT_KODE = "nb"

        fun parse(kode: String?): ApiLocale {
            val verdi = kode?.lowercase() ?: DEFAULT_KODE
            return entries.firstOrNull { it.kode == verdi } ?: NB
        }
    }
}

fun Statuskort.toDto(locale: ApiLocale): StatuskortDto {
    val tekst = locale.velgTekst(innhold)
    return StatuskortDto(
        id = statuskortId,
        tjeneste = tjeneste,
        innhold = TekstinnholdDto(
            tittel = tekst.tittel,
            beskrivelse = tekst.beskrivelse,
            link = tekst.link,
        )
    )
}
