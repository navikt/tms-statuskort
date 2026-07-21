package no.nav.tms.statuskort.statuskort.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tms.common.metrics.installTmsApiMetrics
import no.nav.tms.common.observability.ApiMdc
import no.nav.tms.statuskort.statuskort.Sensitivitet
import no.nav.tms.statuskort.statuskort.Statuskort
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import no.nav.tms.token.support.user.token.verification.userToken
import java.text.DateFormat

private val log = KotlinLogging.logger {}

internal fun Application.statuskortApi(
    statuskortApiRepository: StatuskortApiRepository,
    installAuthenticatorsFunction: Application.() -> Unit = installAuth(),
) {
    installAuthenticatorsFunction()

    install(ApiMdc)

    installTmsApiMetrics {
        setupMetricsRoute = false
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            dateFormat = DateFormat.getDateTimeInstance()
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError)
            log.warn(cause) { "Apikall feiler" }
        }
    }

    routing {
        authenticate {
            get("/statuskort") {
                val user = userPrincipal
                val locale = ApiLocale.parse(call.request.queryParameters["locale"])

                val response = withContext(Dispatchers.IO) {
                    val aktiveKort = statuskortApiRepository.hentAktiveStatuskort(user.ident)
                    byggResponse(aktiveKort, user.levelOfAssurance, locale)
                }

                call.respond(response)
            }
        }
    }
}

private fun byggResponse(
    aktiveKort: List<Statuskort>,
    levelOfAssurance: LevelOfAssurance,
    locale: ApiLocale,
): StatuskortResponse {
    val tillatteKort = aktiveKort.filter { erTilgjengelig(it.sensitivitet, levelOfAssurance) }
    val harSkjulteKort = aktiveKort.any { !erTilgjengelig(it.sensitivitet, levelOfAssurance) }

    return StatuskortResponse(
        statuskort = tillatteKort.map { it.toDto(locale) },
        harSkjulteKort = harSkjulteKort,
    )
}

private fun erTilgjengelig(sensitivitet: Sensitivitet, levelOfAssurance: LevelOfAssurance): Boolean =
    when (sensitivitet) {
        Sensitivitet.Substantial -> true
        Sensitivitet.High -> levelOfAssurance == LevelOfAssurance.High
    }

private fun installAuth(): Application.() -> Unit = {
    authentication {
        userToken {
            levelOfAssurance = LevelOfAssurance.Substantial
        }
    }
}

private val RoutingContext.userPrincipal: UserPrincipal
    get() = call.principal<UserPrincipal>()
        ?: throw IllegalStateException("Fant ikke UserPrincipal i context")
