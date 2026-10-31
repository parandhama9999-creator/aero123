package androidx.compose.runtime

/** The engine's material catalogue is a Compose state map in the app; on the server a plain ordered map is enough. */
fun <K, V> mutableStateMapOf(): MutableMap<K, V> = LinkedHashMap()
