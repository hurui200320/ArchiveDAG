package info.skyblond.archivedag.util

fun getUnixTimestamp(): Long {
    return System.currentTimeMillis() / 1000
}

fun getUnixTimestamp(millis: Long): Long {
    return millis / 1000
}
