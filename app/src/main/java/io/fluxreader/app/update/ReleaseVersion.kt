package io.fluxreader.app.update

/** Comparable GitHub release tags. Pre-release integers are NOT compared lexicographically. */
data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val alpha: Int? = null,
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        if (alpha == null && other.alpha != null) return 1
        if (alpha != null && other.alpha == null) return -1
        return compareValues(alpha, other.alpha)
    }

    companion object {
        private val pattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-alpha\\.(\\d+))?$")
        fun parse(input: String): ReleaseVersion? {
            val match = pattern.matchEntire(input.trim()) ?: return null
            val values = (1..3).map { match.groupValues[it].toIntOrNull() ?: return null }
            val alphaText = match.groupValues[4]
            val alpha = if (alphaText.isNotEmpty()) alphaText.toIntOrNull() ?: return null else null
            return ReleaseVersion(values[0], values[1], values[2], alpha)
        }
    }
}

object UpdateVersionPolicy {
    fun newerThanInstalled(tag: String, installedName: String, includePreReleases: Boolean): Boolean {
        val candidate = ReleaseVersion.parse(tag) ?: return false
        val installed = ReleaseVersion.parse(installedName) ?: return false
        return (includePreReleases || candidate.alpha == null) && candidate > installed
    }
}
