# tms-statuskort

Tjeneste for statuskort på Min side. Konsumerer statuskort-events fra Kafka og lagrer dem i PostgreSQL.

## Eventtyper appen håndterer

Leser fra topic `min-side.statuskort-v1` og ruter på `@event_name`:

- **opprett**: Oppretter nytt statuskort (`statuskortId`, `ident`, `tjeneste`, `innhold`, `sensitivitet`, `produsent`). Duplikat `statuskortId` ignoreres.
- **oppdater**: Oppdaterer `innhold` på et eksisterende statuskort (`statuskortId`, `innhold`). Ukjent `statuskortId` gir feilet melding. Inaktivert statuskort kan ikke oppdateres og gir feilet melding.
- **inaktiver**: Inaktiverer et statuskort (`statuskortId`).

## API

Appen eksponerer et REST-endepunkt for å hente aktive statuskort for
innlogget bruker.

**Eksempel:** `GET /statuskort?locale=nb`

```json
{
  "statuskort": [
    {
      "id": "<statuskortId>",
      "tjeneste": "dagpenger",
      "innhold": {
        "tittel": "...",
        "beskrivelse": "...",
        "link": "..."
      }
    }
  ],
  "harSkjulteKort": false
}
```

## Utvikling

```bash
./gradlew build
```

## Dokumentasjon

TODO: skrive dokumentasjon og legge det til her 

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på github.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-minside
