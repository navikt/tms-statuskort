package no.nav.tms.statuskort.setup

import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = getEnvVar("KAFKA_GROUP_ID"),
    val statuskortTopic: String = getEnvVar("STATUSKORT_TOPIC"),
    val jdbcUrl: String = getEnvVar("DB_JDBC_URL"),
)
