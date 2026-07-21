package no.nav.tms.statuskort.database

import kotliquery.queryOf
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.common.postgres.PostgresDatabase
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

object LocalPostgresDatabase {

    private val container = PostgreSQLContainer("postgres:15").apply { start() }

    val instance: PostgresDatabase by lazy {
        Postgres.connectToContainer(container).also { migrate(it, expectedMigrations = 1) }
    }

    fun cleanDb(): PostgresDatabase {
        instance.update { queryOf("delete from statuskort") }
        instance.update { queryOf("delete from statuskort_event_historikk") }
        return instance
    }

    private fun migrate(database: PostgresDatabase, expectedMigrations: Int) {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(database.dataSource)
            .load()
            .migrate()
            .let { assert(it.migrationsExecuted == expectedMigrations) }
    }
}
