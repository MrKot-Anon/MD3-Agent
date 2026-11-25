package dev.huecoder.md3agent.system.shell

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

/** Extracts the pinned Debian OCI layer while preserving symlinks and usable hardlink contents. */
internal object DebianArchiveExtractor {
    fun extract(archive: File, destination: File) {
        val root = destination.toPath().toAbsolutePath().normalize()
        val hardlinks = mutableListOf<Triple<Path, Path, Int>>()
        val directories = mutableListOf<Pair<File, Int>>()
        TarArchiveInputStream(GZIPInputStream(archive.inputStream())).use { tar ->
            while (true) {
                val entry = tar.nextEntry ?: break
                val path = safePath(root, entry.name)
                when {
                    entry.isDirectory -> {
                        Files.createDirectories(path)
                        directories += path.toFile() to entry.mode
                    }
                    entry.isSymbolicLink -> {
                        Files.createDirectories(path.parent)
                        Files.deleteIfExists(path)
                        Files.createSymbolicLink(path, Path.of(entry.linkName))
                    }
                    entry.isLink -> hardlinks += Triple(path, safePath(root, entry.linkName), entry.mode)
                    entry.isFile -> {
                        Files.createDirectories(path.parent)
                        Files.deleteIfExists(path)
                        path.toFile().outputStream().use { output -> tar.copyTo(output) }
                        setMode(path.toFile(), entry.mode)
                    }
                }
            }
        }
        for ((link, target, mode) in hardlinks) {
            Files.createDirectories(link.parent)
            Files.deleteIfExists(link)
            try { Files.createLink(link, target) } catch (_: Exception) { Files.copy(target, link) }
            setMode(link.toFile(), mode)
        }
        directories.asReversed().forEach { (directory, mode) -> setMode(directory, mode) }
    }

    private fun safePath(root: Path, name: String): Path {
        val relative = name.removePrefix("./")
        if (relative.startsWith("/")) throw IOException("Absolute path in Debian archive")
        val path = root.resolve(relative).normalize()
        if (!path.startsWith(root)) throw IOException("Path escapes Debian archive")
        return path
    }

    private fun setMode(file: File, mode: Int) {
        file.setReadable(mode and 0b100100100 != 0, false)
        file.setWritable(mode and 0b010010010 != 0, false)
        file.setExecutable(mode and 0b001001001 != 0, false)
    }
}
