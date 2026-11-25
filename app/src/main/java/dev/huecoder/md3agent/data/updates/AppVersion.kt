package dev.huecoder.md3agent.data.updates

class AppVersion private constructor(private val parts: List<Int>, private val localRevision: Int, val label: String) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        val length = maxOf(parts.size, other.parts.size)
        for (index in 0 until length) {
            val difference = parts.getOrElse(index) { 0 } - other.parts.getOrElse(index) { 0 }
            if (difference != 0) return difference
        }
        return localRevision.compareTo(other.localRevision)
    }

    override fun equals(other: Any?) = other is AppVersion && compareTo(other) == 0

    override fun hashCode() = 31 * parts.dropLastWhile { it == 0 }.hashCode() + localRevision

    override fun toString() = label

    companion object {
        private val NUMERIC = Regex("""^v?(\d+(?:\.\d+)*)(?:-local\.(\d+))?""")

        fun parse(raw: String): AppVersion? {
            val match = NUMERIC.find(raw.trim()) ?: return null
            val numbers = match.groupValues[1]
            val revision = match.groupValues[2].toIntOrNull() ?: 0
            return AppVersion(numbers.split('.').map(String::toInt), revision, if (revision > 0) "$numbers-local.$revision" else numbers)
        }
    }
}
