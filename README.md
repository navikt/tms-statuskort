# tms-statuskort

Tjeneste for statuskort på Min side. Konsumerer statuskort-events fra Kafka og lagrer dem i PostgreSQL.

> Statusen nå er en basestruktur (Kafka-konsument + database) med håndtering av opprett/oppdater/inaktiver. API kommer senere.

## Arkitektur

- **Kafka-konsument** (`tms-kafka-tools`) som leser fra topic `min-side.statuskort-v1`.
- **PostgreSQL** med Flyway-migrasjoner for lagring av statuskort.
- Kjører på Nais i `dev-gcp`.

## Eventtyper appen håndterer

Leser fra topic `min-side.statuskort-v1` og ruter på `@event_name`:

- **opprett**: Oppretter nytt statuskort (`statuskortId`, `ident`, `innhold`, `sensitivitet`, `produsent`). Duplikat `statuskortId` ignoreres.
- **oppdater**: Oppdaterer `innhold` på et eksisterende statuskort (`statuskortId`, `innhold`). Ukjent `statuskortId` gir feilet melding.
- **inaktiver**: Inaktiverer et statuskort (`statuskortId`). Allerede inaktivt kort er idempotent; ukjent kort gir feilet melding.

> Feil som forventes (mangler kort, duplikat) kastes som `MessageException` og gir en kontrollert skip (logges, offset committes, ingen retry).

## Bruke biblioteket (kotlin-builder)

Produsent-team som skal sende statuskort-events bruker `kotlin-builder` til å bygge gyldig,
forhåndsvalidert JSON. Biblioteket publiserer ikke til Kafka selv – teamet sender den
returnerte JSON-strengen på topic `min-side.statuskort-v1` med egen Kafka-produsent
(anbefalt: bruk `statuskortId` som kafka-nøkkel for å beholde kronologi per kort).

### Legg til avhengigheten

Artefakt: `no.nav.tms.statuskort:kotlin-builder:<versjon>`

Legg til ett av disse repositoriene i prosjektet:

- `https://maven.pkg.github.com/navikt/tms-statuskort` (GitHub Packages – krever autentisering)
- `https://github-package-registry-mirror.gc.nav.no/cached/maven-release` (NAIS-mirror – krever ingen autentisering)

```kotlin
repositories {
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("no.nav.tms.statuskort:kotlin-builder:<versjon>")
}
```

### Eksempel

```kotlin
import no.nav.tms.statuskort.action.Sensitivitet
import no.nav.tms.statuskort.builder.StatuskortBuilder

val json = StatuskortBuilder.opprett {
    statuskortId = "<uuid>"
    ident = "<fnr>"
    tjeneste = "dagpenger"
    sensitivitet = Sensitivitet.High
    innhold {
        bokmaal { link = "https://nav.no/nb"; tittel = "Tittel"; beskrivelse = "Beskrivelse" }
        nynorsk { link = "https://nav.no/nn"; tittel = "Tittel"; beskrivelse = "Skildring" }
        engelsk { link = "https://nav.no/en"; tittel = "Title"; beskrivelse = "Description" }
    }
}

// Send `json` på topic min-side.statuskort-v1 med egen KafkaProducer,
// gjerne med statuskortId som nøkkel.
```

`produsent` utledes automatisk fra NAIS-miljøvariablene `NAIS_CLUSTER_NAME`,
`NAIS_NAMESPACE` og `NAIS_APP_NAME`, eller kan settes eksplisitt. Bruk `oppdater { ... }`
og `inaktiver { ... }` for de øvrige eventtypene.

### Publisering av biblioteket

Biblioteket publiseres til GitHub Packages av workflowen
`.github/workflows/builder-publish.yaml` når det opprettes en ny GitHub-release. Versjonen
settes fra release-navnet (bruk SemVer, f.eks. `1.0.0`).

## Utvikling

```bash
./gradlew build
```

## Dokumentasjon

Mer info om Min side finnes i [dokumentasjonen](https://navikt.github.io/tms-dokumentasjon/).

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på github.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-minside
