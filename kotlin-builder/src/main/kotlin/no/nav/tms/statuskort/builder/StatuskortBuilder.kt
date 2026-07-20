package no.nav.tms.statuskort.builder

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.tms.statuskort.action.InaktiverStatuskort
import no.nav.tms.statuskort.action.Innhold
import no.nav.tms.statuskort.action.OppdaterStatuskort
import no.nav.tms.statuskort.action.OpprettStatuskort
import no.nav.tms.statuskort.action.Produsent
import no.nav.tms.statuskort.action.Sensitivitet
import no.nav.tms.statuskort.action.StatuskortActionVersion
import no.nav.tms.statuskort.action.Tekstinnhold
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object StatuskortBuilder {
    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    fun opprett(builderFunction: OpprettStatuskortInstance.() -> Unit): String {
        return OpprettStatuskortInstance()
            .also { it.builderFunction() }
            .also { it.performNullCheck() }
            .build()
            .let { objectMapper.writeValueAsString(it) }
    }

    fun oppdater(builderFunction: OppdaterStatuskortInstance.() -> Unit): String {
        return OppdaterStatuskortInstance()
            .also { it.builderFunction() }
            .also { it.performNullCheck() }
            .build()
            .let { objectMapper.writeValueAsString(it) }
    }

    fun inaktiver(builderFunction: InaktiverStatuskortInstance.() -> Unit): String {
        return InaktiverStatuskortInstance()
            .also { it.builderFunction() }
            .also { it.performNullCheck() }
            .build()
            .let { objectMapper.writeValueAsString(it) }
    }

    class OpprettStatuskortInstance internal constructor(
        var statuskortId: String? = null,
        var ident: String? = null,
        var tjeneste: String? = null,
        var sensitivitet: Sensitivitet? = null,
        var produsent: Produsent? = produsent(),
    ) {
        private var innhold: Innhold? = null

        val metadata = metadata()

        fun innhold(builderFunction: InnholdInstance.() -> Unit) {
            innhold = InnholdInstance().apply(builderFunction).build()
        }

        internal fun build() = OpprettStatuskort(
            statuskortId = statuskortId!!,
            ident = ident!!,
            tjeneste = tjeneste!!,
            innhold = innhold!!,
            sensitivitet = sensitivitet!!,
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() {
            requireNotBlank(statuskortId, "statuskortId")
            requireNotBlank(ident, "ident")
            requireNotBlank(tjeneste, "tjeneste")
            requireNotNullField(sensitivitet, "sensitivitet")
            requireNotNullField(
                produsent,
                "produsent (utledes fra NAIS-miljøvariabler eller settes eksplisitt)"
            )
            requireNotNullField(innhold, "innhold")
        }
    }

    class OppdaterStatuskortInstance internal constructor(
        var statuskortId: String? = null,
    ) {
        private var innhold: Innhold? = null

        val metadata = metadata()

        fun innhold(builderFunction: InnholdInstance.() -> Unit) {
            innhold = InnholdInstance().apply(builderFunction).build()
        }

        internal fun build() = OppdaterStatuskort(
            statuskortId = statuskortId!!,
            innhold = innhold!!,
            metadata = metadata
        )

        internal fun performNullCheck() {
            requireNotBlank(statuskortId, "statuskortId")
            requireNotNullField(innhold, "innhold")
        }
    }

    class InaktiverStatuskortInstance internal constructor(
        var statuskortId: String? = null,
    ) {
        val metadata = metadata()

        internal fun build() = InaktiverStatuskort(
            statuskortId = statuskortId!!,
            metadata = metadata
        )

        internal fun performNullCheck() {
            requireNotBlank(statuskortId, "statuskortId")
        }
    }

    class InnholdInstance internal constructor() {
        private var bokmaal: Tekstinnhold? = null
        private var nynorsk: Tekstinnhold? = null
        private var engelsk: Tekstinnhold? = null

        fun bokmaal(builderFunction: TekstinnholdInstance.() -> Unit) {
            bokmaal = TekstinnholdInstance().apply(builderFunction).build("innhold.bokmaal")
        }

        fun nynorsk(builderFunction: TekstinnholdInstance.() -> Unit) {
            nynorsk = TekstinnholdInstance().apply(builderFunction).build("innhold.nynorsk")
        }

        fun engelsk(builderFunction: TekstinnholdInstance.() -> Unit) {
            engelsk = TekstinnholdInstance().apply(builderFunction).build("innhold.engelsk")
        }

        internal fun build() = Innhold(
            bokmaal = requireNotNullField(bokmaal, "innhold.bokmaal"),
            nynorsk = requireNotNullField(nynorsk, "innhold.nynorsk"),
            engelsk = requireNotNullField(engelsk, "innhold.engelsk"),
        )
    }

    class TekstinnholdInstance internal constructor(
        var link: String? = null,
        var tittel: String? = null,
        var beskrivelse: String? = null,
    ) {
        internal fun build(prefix: String) = Tekstinnhold(
            link = requireNotBlank(link, "$prefix.link"),
            tittel = requireNotBlank(tittel, "$prefix.tittel"),
            beskrivelse = requireNotBlank(beskrivelse, "$prefix.beskrivelse"),
        )
    }

    private fun produsent(): Produsent? {
        val cluster: String? = BuilderEnvironment.get("NAIS_CLUSTER_NAME")
        val namespace: String? = BuilderEnvironment.get("NAIS_NAMESPACE")
        val appnavn: String? = BuilderEnvironment.get("NAIS_APP_NAME")

        return if (cluster.isNullOrBlank() || namespace.isNullOrBlank() || appnavn.isNullOrBlank()) {
            null
        } else {
            Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = appnavn
            )
        }
    }

    private fun metadata() = mutableMapOf<String, Any>(
        "version" to StatuskortActionVersion,
        "built_at" to ZonedDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.MILLIS),
        "builder_lang" to "kotlin"
    )
}
