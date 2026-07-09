package no.nav.tms.statuskort.statuskort

fun opprettEvent(
    statuskortId: String,
    ident: String = "12345678901",
    tjeneste: String = "dagpenger",
    sensitivitet: String = "high",
    tittel: String = "En tittel",
): String = """
    {
        "@event_name": "opprett",
        "statuskortId": "$statuskortId",
        "ident": "$ident",
        "tjeneste": "$tjeneste",
        "innhold": ${innholdJson(tittel)},
        "sensitivitet": "$sensitivitet",
        "produsent": {
            "cluster": "dev-gcp",
            "namespace": "min-side",
            "appnavn": "test-app"
        }
    }
""".trimIndent()

fun oppdaterEvent(
    statuskortId: String,
    tittel: String = "Oppdatert tittel",
): String = """
    {
        "@event_name": "oppdater",
        "statuskortId": "$statuskortId",
        "innhold": ${innholdJson(tittel)}
    }
""".trimIndent()

fun inaktiverEvent(
    statuskortId: String,
): String = """
    {
        "@event_name": "inaktiver",
        "statuskortId": "$statuskortId"
    }
""".trimIndent()

private fun innholdJson(tittel: String): String = """
    {
        "bokmaal": { "link": "https://nav.no/nb", "tittel": "$tittel", "beskrivelse": "Beskrivelse bokmål" },
        "nynorsk": { "link": "https://nav.no/nn", "tittel": "$tittel", "beskrivelse": "Skildring nynorsk" },
        "engelsk": { "link": "https://nav.no/en", "tittel": "$tittel", "beskrivelse": "Description english" }
    }
""".trimIndent()
