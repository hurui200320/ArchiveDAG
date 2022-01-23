package info.skyblond.archivedag.commons

fun getUnixTimestamp(): Long = getUnixTimestamp(System.currentTimeMillis())

fun getUnixTimestamp(millis: Long): Long {
    return millis / 1000
}
