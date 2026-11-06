package is.xyz.mpv

import android.content.Context
import android.content.SharedPreferences

/**
 * Keeps track of which files were played last and which ones were watched to the end,
 * so that the file pickers can highlight them.
 *
 * Files are identified by a "key": the absolute path for normal files, or the URI string
 * for content:// documents and network streams.
 */
object PlaybackHistory {
    private const val PREFS_NAME = "playback_history"
    private const val KEY_LAST_PLAYED = "last_played"
    private const val KEY_WATCHED = "watched"

    /** Fraction of the duration after which a file counts as watched */
    const val WATCHED_THRESHOLD = 0.95

    /** Upper bound on remembered watched files, to keep the preferences small */
    private const val MAX_WATCHED = 5000

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Keys of the most recently played file (may contain aliases of the same file). */
    @JvmStatic
    fun getLastPlayed(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_LAST_PLAYED, null)?.toSet() ?: emptySet()

    @JvmStatic
    fun getWatched(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_WATCHED, null)?.toSet() ?: emptySet()

    @JvmStatic
    fun setLastPlayed(context: Context, keys: Collection<String>) {
        if (keys.isEmpty())
            return
        prefs(context).edit().putStringSet(KEY_LAST_PLAYED, keys.toHashSet()).apply()
    }

    @Synchronized
    @JvmStatic
    fun markWatched(context: Context, keys: Collection<String>) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_WATCHED, null)?.toHashSet() ?: hashSetOf()
        if (!set.addAll(keys))
            return
        // StringSet has no order, so just drop arbitrary entries if we get too big
        if (set.size > MAX_WATCHED) {
            val excess = set.filter { it !in keys }.take(set.size - MAX_WATCHED)
            set.removeAll(excess.toSet())
        }
        p.edit().putStringSet(KEY_WATCHED, set).apply()
    }
}
