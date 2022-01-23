package info.skyblond.archivedag

import org.junit.jupiter.api.function.Executable

fun safeExecutable(executable: Executable) {
    try {
        executable.execute()
    } catch (ignored: Throwable) {
    }
}
