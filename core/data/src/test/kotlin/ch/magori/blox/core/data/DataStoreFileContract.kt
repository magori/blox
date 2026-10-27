package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The contract of the errors of a DataStore file, which every repository [R] on a file of its own keeps: an unreadable
 * file reads as an empty one and is written again, a file that cannot be read (an IOException) reads as an empty one,
 * and any other failure is a bug that goes through. An interface, so that the test of a repository, which already
 * extends the contract of that repository, proves it too.
 */
interface DataStoreFileContract<R> {
    /** The file of the repository, in the temporary directory of the test. */
    val file: File

    /** A repository on [file], its DataStore living in [scope], as in production. */
    fun repositoryIn(scope: CoroutineScope): R

    /** A repository on a DataStore whose every read and write fails with [failure], a bug. */
    fun repositoryFailingWith(failure: Exception): R

    /** What [repository] reads from its file: each of its flows. */
    fun readsOf(repository: R): List<Flow<Any?>>

    /** What each flow of [readsOf] gives first when the file is empty, in their order. */
    val emptyReads: List<Any?>

    /** Writes something with [repository], and checks what the write answers, if it answers. */
    suspend fun write(repository: R)

    /** What each flow of [readsOf] gives first after [write] on an empty file, in their order. */
    val readsAfterWrite: List<Any?>

    @Test
    fun `an unreadable file reads as an empty one, and what is written next is kept`() = runTest {
        // Given a file that is not one of its kind
        file.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(firstReadsOf(repository)).isEqualTo(emptyReads)
        write(repository)
        assertThat(firstReadsOf(repository)).isEqualTo(readsAfterWrite)
    }

    @Test
    fun `a file that cannot be read reads as an empty one`() = runTest {
        // Given a directory where the file should be: reading it fails with an IOException
        file.mkdirs()
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(firstReadsOf(repository)).isEqualTo(emptyReads)
    }

    @Test
    fun `a failure other than an IOException while the file is read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val repository = repositoryFailingWith(IllegalStateException("a bug"))

        // When
        val failures = readsOf(repository).map { runCatching { it.first() }.exceptionOrNull() }

        // Then
        assertThat(failures).isNotEmpty().allSatisfy {
            assertThat(it).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
        }
    }

    /** What each flow of [repository] gives first. */
    private suspend fun firstReadsOf(repository: R): List<Any?> = readsOf(repository).map { it.first() }
}
