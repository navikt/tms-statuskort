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
