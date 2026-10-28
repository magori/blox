package ch.magori.blox.di

import ch.magori.blox.core.data.DAILY_CHALLENGE_FILE
import ch.magori.blox.core.data.GARDEN_FILE
import ch.magori.blox.core.data.MUSICS_FILE
import ch.magori.blox.core.data.MY_MUSIC_FILE
import ch.magori.blox.core.data.RECORDS_FILE
import ch.magori.blox.core.data.SETTINGS_FILE
import ch.magori.blox.theme.ANDROID_NAMESPACE
import ch.magori.blox.theme.appManifest
import ch.magori.blox.theme.appResource
import ch.magori.blox.theme.element
import ch.magori.blox.theme.elements
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import java.io.File

class BackupRulesTest {
    @Test
    fun `the manifest gives the backup rules of Android 12 and those of the older versions`() {
        // Given
        val application = element(appManifest(), "/manifest/application")

        // Then
        assertThat(application.getAttributeNS(ANDROID_NAMESPACE, "dataExtractionRules"))
            .isEqualTo("@xml/data_extraction_rules")
        assertThat(application.getAttributeNS(ANDROID_NAMESPACE, "fullBackupContent"))
            .isEqualTo("@xml/backup_rules")
    }

    @Test
    fun `from Android 12, the cloud and device backups keep only the record, the settings, the musics, the garden`() {
        // Given
        val rules = appResource("xml/data_extraction_rules.xml")

        // Then
        listOf("cloud-backup", "device-transfer").forEach { section ->
            assertThat(included(rules, "/data-extraction-rules/$section"))
                .withFailMessage { "The $section of data_extraction_rules.xml does not keep exactly $DATASTORE_FILES" }
                .containsExactlyInAnyOrderElementsOf(DATASTORE_FILES)
        }
    }

    @Test
    fun `before Android 12, the backup keeps only the record, the settings, the musics, the garden`() {
        // Given
        val rules = appResource("xml/backup_rules.xml")

        // Then
        assertThat(included(rules, "/full-backup-content"))
            .withFailMessage { "backup_rules.xml does not keep exactly $DATASTORE_FILES" }
            .containsExactlyInAnyOrderElementsOf(DATASTORE_FILES)
    }

    @Test
    fun `the music file of my file never leaves the device`() {
        // Given the file of « Mon fichier », whose uri names a document of the player and whose permission belongs to
        // this device
        val myMusicFile = datastorePath(MY_MUSIC_FILE)

        // Then no backup takes it, whatever the version of Android
        everyBackup().forEach { assertThat(it).doesNotContain(myMusicFile) }
    }

    @Test
    fun `the file of the daily challenge never leaves the device`() {
        // Given the file of the results of the day, whose first try would mean nothing on another phone
        val dailyChallengeFile = datastorePath(DAILY_CHALLENGE_FILE)

        // Then no backup takes it, whatever the version of Android: no include names it, nor a directory holding it
        everyBackup().flatten().forEach { include ->
            assertThat(dailyChallengeFile == include || dailyChallengeFile.startsWith(include.removeSuffix("/") + "/"))
                .withFailMessage { "The include $include takes $dailyChallengeFile" }
                .isFalse()
        }
    }

    /** The files that each backup includes: the cloud backup and the device transfer from Android 12, then before. */
    private fun everyBackup(): List<List<String>> {
        val rules = appResource("xml/data_extraction_rules.xml")
        val sections = listOf("cloud-backup", "device-transfer").map { included(rules, "/data-extraction-rules/$it") }
        return sections + listOf(included(appResource("xml/backup_rules.xml"), "/full-backup-content"))
    }

    /** The files that the rules under [section] include, as `domain:path`. */
    private fun included(
        rules: Document,
        section: String,
    ): List<String> = elements(rules, "$section/include").map { include ->
        "${include.getAttribute("domain")}:${include.getAttribute("path")}"
    }

    private companion object {
        /** The DataStore files of core:data, where the application creates them, relative to its files directory. */
        val DATASTORE_FILES = listOf(RECORDS_FILE, SETTINGS_FILE, MUSICS_FILE, GARDEN_FILE).map(::datastorePath)

        /** Where the application creates the DataStore file [name], as a rule names it: `file:<path>`. */
        fun datastorePath(name: String): String {
            val filesDir = File("files")
            return "file:" + dataStoreFile(filesDir, name).toFile().relativeTo(filesDir).invariantSeparatorsPath
        }
    }
}
