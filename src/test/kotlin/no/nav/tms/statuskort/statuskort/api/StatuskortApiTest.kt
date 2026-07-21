package no.nav.tms.statuskort.statuskort.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.server.auth.authentication
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tms.statuskort.database.LocalPostgresDatabase
import no.nav.tms.statuskort.statuskort.Innhold
import no.nav.tms.statuskort.statuskort.Produsent
import no.nav.tms.statuskort.statuskort.Sensitivitet
import no.nav.tms.statuskort.statuskort.Statuskort
import no.nav.tms.statuskort.statuskort.StatuskortRepository
import no.nav.tms.statuskort.statuskort.Tekstinnhold
import no.nav.tms.statuskort.statuskort.ZonedDateTimeHelper
import no.nav.tms.token.support.user.token.verification.Issuer
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verificaton.mock.userTokenMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class StatuskortApiTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val writeRepository = StatuskortRepository(database)
    private val apiRepository = StatuskortApiRepository(database)
    private val objectMapper = jacksonObjectMapper()

    private val testIdent = "00011"
    private val annenIdent = "00022"

    @AfterEach
    fun cleanUp() {
        LocalPostgresDatabase.cleanDb()
    }

    @Test
    fun `henter aktive statuskort for innlogget bruker paa bokmaal som default`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "kort-1", ident = testIdent, tjeneste = "dagpenger")

            val body = client.get("/statuskort").parse()

            body.get("harSkjulteKort").asBoolean() shouldBe false
            val kort = body.get("statuskort")
            kort.size() shouldBe 1
            kort[0].get("id").asText() shouldBe "kort-1"
            kort[0].get("tjeneste").asText() shouldBe "dagpenger"
            kort[0].get("innhold").get("link").asText() shouldBe "https://nav.no/nb"
            kort[0].get("innhold").has("nb") shouldBe false
        }

    @Test
    fun `velger tekst basert paa locale`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "kort-1", ident = testIdent)

            val nn = client.get("/statuskort?locale=nn").parse()
            nn.get("statuskort")[0].get("innhold").get("link").asText() shouldBe "https://nav.no/nn"

            val en = client.get("/statuskort?locale=en").parse()
            en.get("statuskort")[0].get("innhold").get("link").asText() shouldBe "https://nav.no/en"
        }

    @Test
    fun `ugyldig locale faller tilbake til nb`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "kort-1", ident = testIdent)

            val body = client.get("/statuskort?locale=de").parse()

            body.get("statuskort")[0].get("innhold").get("link").asText() shouldBe "https://nav.no/nb"
        }

    @Test
    fun `returnerer kun kort for identen i tokenet`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "mitt", ident = testIdent)
            lagreStatuskort(statuskortId = "annet", ident = annenIdent)

            val body = client.get("/statuskort").parse()

            body.get("statuskort").map { it.get("id").asText() }
                .shouldContainExactlyInAnyOrder("mitt")
        }

    @Test
    fun `returnerer kun aktive kort`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "aktivt", ident = testIdent, aktiv = true)
            lagreStatuskort(statuskortId = "inaktivt", ident = testIdent, aktiv = false)

            val body = client.get("/statuskort").parse()

            body.get("statuskort").map { it.get("id").asText() }
                .shouldContainExactlyInAnyOrder("aktivt")
        }

    @Test
    fun `substantial-bruker faar ikke high-kort og faar flagg om skjulte kort`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.Substantial) {
            lagreStatuskort(statuskortId = "sub", ident = testIdent, sensitivitet = Sensitivitet.Substantial)
            lagreStatuskort(statuskortId = "high", ident = testIdent, sensitivitet = Sensitivitet.High)

            val body = client.get("/statuskort").parse()

            body.get("statuskort").map { it.get("id").asText() }
                .shouldContainExactlyInAnyOrder("sub")
            body.get("harSkjulteKort").asBoolean() shouldBe true
        }

    @Test
    fun `high-bruker faar alle kort uten flagg`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.High) {
            lagreStatuskort(statuskortId = "sub", ident = testIdent, sensitivitet = Sensitivitet.Substantial)
            lagreStatuskort(statuskortId = "high", ident = testIdent, sensitivitet = Sensitivitet.High)

            val body = client.get("/statuskort").parse()

            body.get("statuskort").map { it.get("id").asText() }
                .shouldContainExactlyInAnyOrder("sub", "high")
            body.get("harSkjulteKort").asBoolean() shouldBe false
        }

    @Test
    fun `substantial-bruker uten high-kort faar ikke flagg`() =
        statuskortTestApplication(testIdent, LevelOfAssurance.Substantial) {
            lagreStatuskort(statuskortId = "sub", ident = testIdent, sensitivitet = Sensitivitet.Substantial)

            val body = client.get("/statuskort").parse()

            body.get("statuskort").size() shouldBe 1
            body.get("harSkjulteKort").asBoolean() shouldBe false
        }

    private suspend fun HttpResponse.parse(): JsonNode =
        objectMapper.readTree(bodyAsText())

    private fun lagreStatuskort(
        statuskortId: String,
        ident: String,
        tjeneste: String = "dagpenger",
        sensitivitet: Sensitivitet = Sensitivitet.Substantial,
        aktiv: Boolean = true,
    ) {
        val naa = ZonedDateTimeHelper.nowAtUtc()
        writeRepository.opprettStatuskort(
            Statuskort(
                statuskortId = statuskortId,
                ident = ident,
                tjeneste = tjeneste,
                innhold = Innhold(
                    bokmaal = Tekstinnhold("https://nav.no/nb", "Tittel", "Beskrivelse"),
                    nynorsk = Tekstinnhold("https://nav.no/nn", "Tittel", "Skildring"),
                    engelsk = Tekstinnhold("https://nav.no/en", "Title", "Description"),
                ),
                sensitivitet = sensitivitet,
                produsent = Produsent("dev-gcp", "min-side", "test-app"),
                aktiv = aktiv,
                opprettet = naa,
                sistEndret = naa,
                inaktivert = if (aktiv) null else naa,
            )
        )
    }

    private fun statuskortTestApplication(
        ident: String,
        levelOfAssurance: LevelOfAssurance,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            statuskortApi(
                statuskortApiRepository = apiRepository,
                installAuthenticatorsFunction = {
                    authentication {
                        userTokenMock {
                            this.levelOfAssurance = LevelOfAssurance.Substantial
                            enableDefaultAuthentication {
                                tokenIssuer = Issuer.IdPorten
                                tokenIdent = ident
                                tokenLoa = levelOfAssurance
                            }
                        }
                    }
                }
            )
        }
        block()
    }
}
