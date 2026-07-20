package no.nav.tms.statuskort.builder

object BuilderEnvironment {
    private val baseEnv = System.getenv()
    private val env = mutableMapOf<String, String>()

    init {
        env.putAll(baseEnv)
    }

    fun extend(extendedEnv: Map<String, String>) {
        env.putAll(extendedEnv)
    }

    fun reset() {
        env.clear()
        env.putAll(baseEnv)
    }

    internal fun get(name: String): String? = env[name]
}
