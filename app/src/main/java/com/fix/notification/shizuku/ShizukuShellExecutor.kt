package com.fix.notification.shizuku

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Result of a shell command executed via Shizuku.
 * Separates stdout, stderr, and exitCode so callers know when a command actually fails.
 */
data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
) {
    val isSuccess: Boolean get() = exitCode == 0 && stderr.isBlank()

    /** Short error message for log display, null if successful. */
    val errorMessage: String?
        get() = when {
            isSuccess -> null
            stderr.isNotBlank() -> stderr.lines().firstOrNull { it.isNotBlank() }?.trim()
            else -> "exit code $exitCode"
        }

    companion object {
        fun failure(reason: String) = ShellResult("", reason, -1)
    }
}

object ShizukuShellExecutor {

    /** Delimiter between commands when running in batch mode. */
    private const val BATCH_SEPARATOR = "___FIXNOTI_CMD_SEP___"
    private const val BATCH_EXIT_MARKER = "___FIXNOTI_EXIT___"

    private var newProcessMethod: Method? = null

    init {
        findNewProcessMethod()
    }

    private fun findNewProcessMethod() {
        try {
            val methods = Shizuku::class.java.declaredMethods
            for (m in methods) {
                if (m.name == "newProcess") {
                    m.isAccessible = true
                    newProcessMethod = m
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Executes a single command and returns trimmed stdout.
     * Kept for backwards compatibility; prefer [run] if exit status/stderr is required.
     */
    fun executeCommand(command: String): String = run(command).stdout

    /** Executes a single command and returns complete ShellResult with stdout/stderr/exitCode. */
    fun run(command: String): ShellResult {
        if (!isPermissionGranted()) {
            return ShellResult.failure("Shizuku permission not granted")
        }

        return try {
            if (newProcessMethod == null) findNewProcessMethod()

            val process = newProcessMethod?.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
                ?: return ShellResult.failure("Failed to invoke Shizuku newProcess")

            // Read stdout and stderr in parallel to prevent buffer deadlocks.
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val errThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        reader.forEachLine { stderr.append(it).append('\n') }
                    }
                } catch (ignored: Throwable) {
                }
            }
            errThread.start()

            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.forEachLine { stdout.append(it).append('\n') }
                }
            } catch (ignored: Throwable) {
            }

            // Must use plain waitFor(). Do NOT use waitFor(timeout, TimeUnit),
            // which repeatedly queries exitValue() and causes IllegalThreadStateException on ShizukuRemoteProcess.
            val exitCode = process.waitFor()
            errThread.join(1000)

            ShellResult(
                stdout = stdout.toString().trim(),
                stderr = stderr.toString().trim(),
                exitCode = exitCode
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            ShellResult.failure(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }

    /**
     * Runs multiple commands inside a SINGLE shell process, splitting outputs and exit codes.
     * Spawning a binder process is expensive; batching reduces hundreds of spawns to 1.
     */
    fun runBatch(commands: List<String>): List<BatchResult> {
        if (commands.isEmpty()) return emptyList()
        if (!isPermissionGranted()) {
            return List(commands.size) { BatchResult("Shizuku permission not granted", -1) }
        }

        val script = commands.joinToString("\n") { cmd ->
            "echo $BATCH_SEPARATOR\n{ $cmd ; } 2>&1\necho $BATCH_EXIT_MARKER\$?"
        }

        val output = run(script).stdout
        val parts = output.split(BATCH_SEPARATOR)

        return List(commands.size) { index ->
            val raw = parts.getOrNull(index + 1)
                ?: return@List BatchResult("No output received", -1)

            val markerAt = raw.lastIndexOf(BATCH_EXIT_MARKER)
            if (markerAt < 0) {
                BatchResult(raw.trim(), -1)
            } else {
                val code = raw.substring(markerAt + BATCH_EXIT_MARKER.length).trim().toIntOrNull() ?: -1
                BatchResult(raw.substring(0, markerAt).trim(), code)
            }
        }
    }
}

/** Result of a single command in a batch run. [output] contains combined stdout and stderr. */
data class BatchResult(val output: String, val exitCode: Int) {

    /**
     * Many Android commands (e.g., `appops set`, `settings put`) exit with code 0 even when printing errors,
     * so we verify both exitCode and failure text markers.
     */
    val isSuccess: Boolean
        get() = exitCode == 0 && FAILURE_MARKERS.none { output.contains(it, ignoreCase = true) }

    val errorMessage: String?
        get() = if (isSuccess) null
        else output.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "exit code $exitCode"

    private companion object {
        val FAILURE_MARKERS = listOf(
            "Exception",
            "Error:",
            "error:",
            "Failure",
            "Unknown command",
            "not found",
            "Permission Denial",
            "Bad "
        )
    }
}
