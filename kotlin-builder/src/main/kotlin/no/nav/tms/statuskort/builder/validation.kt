package no.nav.tms.statuskort.builder

import no.nav.tms.statuskort.action.StatuskortValidationException

internal fun requireNotBlank(value: String?, field: String): String {
    if (value.isNullOrBlank()) {
        throw StatuskortValidationException("$field kan ikke være tom eller null")
    }
    return value
}

internal fun <T> requireNotNullField(value: T?, field: String): T {
    return value ?: throw StatuskortValidationException("$field kan ikke være null")
}
