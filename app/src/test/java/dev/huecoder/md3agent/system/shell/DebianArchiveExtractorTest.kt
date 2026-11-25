package dev.huecoder.md3agent.system.shell

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class DebianArchiveExtractorTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun bundledLayerExtractsExecutablesAndLinks() {
        val archive = File("src/main/assets/debian/rootfs-arm64.blob")
        val root = temp.newFolder("debian")
        DebianArchiveExtractor.extract(archive, root)
        assertTrue(File(root, "usr/bin/bash").canExecute())
        assertTrue(File(root, "usr/bin/apt").canExecute())
        assertTrue(Files.isSymbolicLink(File(root, "bin").toPath()))
        assertArrayEquals(File(root, "usr/bin/perl").readBytes(), File(root, "usr/bin/perl5.40.1").readBytes())
    }
}
