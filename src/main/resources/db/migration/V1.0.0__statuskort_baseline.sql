create table statuskort
(
    statuskortId text not null primary key,
    ident        text not null,
    tjeneste     text not null,
    innhold      jsonb not null,
    sensitivitet text not null,
    produsent    jsonb not null,
    aktiv        bool not null,
    inaktivert   timestamp with time zone,
    opprettet    timestamp with time zone not null,
    sistEndret   timestamp with time zone not null
);

create index statuskort_ident on statuskort (ident);
create index statuskort_opprettet on statuskort (opprettet);

create table statuskort_event_historikk
(
    statuskortId text not null,
    ident text not null,
    eventType text not null,
    konsumert timestamp with time zone not null
);
create index statuskort_historikk_ident on statuskort_event_historikk (ident);
