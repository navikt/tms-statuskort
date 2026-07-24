create table statuskort
(
    statuskortId text not null primary key,
    ident        text not null,
    tjeneste     text not null,
    innhold      jsonb not null,
    sensitivitet text not null,
    produsent    jsonb not null,
    aktiv        bool not null,
    opprettet    timestamp with time zone not null,
    sistEndret   timestamp with time zone not null,
    inaktivert   timestamp with time zone
);

create index statuskort_ident on statuskort (ident);
create index statuskort_opprettet on statuskort (opprettet);

create table statuskort_event_historikk
(
    id            serial primary key,
    statuskortId  text not null references statuskort(statuskortId),
    eventType     text not null,
    innhold       jsonb,
    tidspunkt     timestamp with time zone not null
);
