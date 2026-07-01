# tms-statuskort

Tjeneste for statuskort på Min side. Konsumerer statuskort-events fra Kafka og lagrer dem i PostgreSQL.

> Statusen nå er en basestruktur (Kafka-konsument + database). Forretningslogikk og API kommer senere.

## Arkitektur

- **Kafka-konsument** (`tms-kafka-tools`) som leser fra topic `min-side.statuskort-v1`.
- **PostgreSQL** med Flyway-migrasjoner for lagring av statuskort.
- Kjører på Nais i `dev-gcp`.

## Eventtyper appen håndterer

- **statuskort**: Lytter etter statuskort-events. (Håndtering ikke implementert ennå.)

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
