package no.nav.tms.statuskort.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.tms.statuskort.action.StatuskortValidationException

class StatuskortBuilderTest : StringSpec({

    val objectMapper: ObjectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .build()

    beforeEach {
        BuilderEnvironment.extend(
            mapOf(
                "NAIS_CLUSTER_NAME" to "dev-gcp",
                "NAIS_NAMESPACE" to "min-side",
                "NAIS_APP_NAME" to "test-app"
            )
        )
    }

    afterEach {
        BuilderEnvironment.reset()
    }

    fun gyldigInnhold(): StatuskortBuilder.InnholdInstance.() -> Unit = {
        bokmaal {
            link = "https://nav.no/nb"
            tittel = "Tittel"
            beskrivelse = "Beskrivelse"
        }
        nynorsk {
            link = "https://nav.no/nn"
            tittel = "Tittel"
            beskrivelse = "Skildring"
        }
        engelsk {
            link = "https://nav.no/en"
            tittel = "Title"
            beskrivelse = "Description"
        }
    }

    "opprett bygger gyldig event med alle felt" {
        val json = StatuskortBuilder.opprett {
            statuskortId = "123"
            ident = "12345678901"
            tjeneste = "dagpenger"
            sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
            innhold(gyldigInnhold())
        }

        val node: JsonNode = objectMapper.readTree(json)

        node["@event_name"].asText() shouldBe "opprett"
        node["statuskortId"].asText() shouldBe "123"
        node["ident"].asText() shouldBe "12345678901"
        node["tjeneste"].asText() shouldBe "dagpenger"
        node["sensitivitet"].asText() shouldBe "high"
        node["innhold"]["bokmaal"]["tittel"].asText() shouldBe "Tittel"
        node["innhold"]["nynorsk"]["beskrivelse"].asText() shouldBe "Skildring"
        node["innhold"]["engelsk"]["link"].asText() shouldBe "https://nav.no/en"
    }

    "opprett utleder produsent fra NAIS-miljøvariabler" {
        val json = StatuskortBuilder.opprett {
            statuskortId = "123"
            ident = "12345678901"
            tjeneste = "dagpenger"
            sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.Substantial
            innhold(gyldigInnhold())
        }

        val node = objectMapper.readTree(json)

        node["produsent"]["cluster"].asText() shouldBe "dev-gcp"
        node["produsent"]["namespace"].asText() shouldBe "min-side"
        node["produsent"]["appnavn"].asText() shouldBe "test-app"
        node["sensitivitet"].asText() shouldBe "substantial"
    }

    "opprett setter metadata" {
        val json = StatuskortBuilder.opprett {
            statuskortId = "123"
            ident = "12345678901"
            tjeneste = "dagpenger"
            sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
            innhold(gyldigInnhold())
        }

        val metadata = objectMapper.readTree(json)["metadata"]

        metadata["version"].asText() shouldBe "1.0.0"
        metadata["builder_lang"].asText() shouldBe "kotlin"
        metadata["built_at"].asText().isNotBlank() shouldBe true
    }

    "opprett kaster ved manglende statuskortId" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.opprett {
                ident = "12345678901"
                tjeneste = "dagpenger"
                sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
                innhold(gyldigInnhold())
            }
        }
    }

    "opprett kaster ved blank ident" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.opprett {
                statuskortId = "123"
                ident = "   "
                tjeneste = "dagpenger"
                sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
                innhold(gyldigInnhold())
            }
        }
    }

    "opprett kaster ved blank tittel i innhold" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.opprett {
                statuskortId = "123"
                ident = "12345678901"
                tjeneste = "dagpenger"
                sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
                innhold {
                    bokmaal {
                        link = "https://nav.no/nb"
                        tittel = ""
                        beskrivelse = "Beskrivelse"
                    }
                    nynorsk {
                        link = "https://nav.no/nn"
                        tittel = "Tittel"
                        beskrivelse = "Skildring"
                    }
                    engelsk {
                        link = "https://nav.no/en"
                        tittel = "Title"
                        beskrivelse = "Description"
                    }
                }
            }
        }
    }

    "opprett kaster ved manglende språk i innhold" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.opprett {
                statuskortId = "123"
                ident = "12345678901"
                tjeneste = "dagpenger"
                sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
                innhold {
                    bokmaal {
                        link = "https://nav.no/nb"
                        tittel = "Tittel"
                        beskrivelse = "Beskrivelse"
                    }
                }
            }
        }
    }

    "opprett kaster når produsent ikke kan utledes" {
        BuilderEnvironment.reset()
        BuilderEnvironment.extend(emptyMap())

        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.opprett {
                statuskortId = "123"
                ident = "12345678901"
                tjeneste = "dagpenger"
                sensitivitet = no.nav.tms.statuskort.action.Sensitivitet.High
                produsent = null
                innhold(gyldigInnhold())
            }
        }
    }

    "oppdater bygger gyldig event" {
        val json = StatuskortBuilder.oppdater {
            statuskortId = "123"
            innhold(gyldigInnhold())
        }

        val node = objectMapper.readTree(json)

        node["@event_name"].asText() shouldBe "oppdater"
        node["statuskortId"].asText() shouldBe "123"
        node["innhold"]["bokmaal"]["tittel"].asText() shouldBe "Tittel"
    }

    "oppdater kaster ved manglende innhold" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.oppdater {
                statuskortId = "123"
            }
        }
    }

    "inaktiver bygger gyldig event" {
        val json = StatuskortBuilder.inaktiver {
            statuskortId = "123"
        }

        val node = objectMapper.readTree(json)

        node["@event_name"].asText() shouldBe "inaktiver"
        node["statuskortId"].asText() shouldBe "123"
    }

    "inaktiver kaster ved manglende statuskortId" {
        shouldThrow<StatuskortValidationException> {
            StatuskortBuilder.inaktiver { }
        }
    }
})
