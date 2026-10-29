package io.fluxreader.app.data

import android.content.ContextWrapper
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Process
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryImportDestinationTest {
    @Test fun filesAndExistingBooksAreAddedToTheRequestedSubfolderWithoutDuplicatingFiles() = runBlocking {
        val context = isolatedContext()
        val repository = LibraryRepository(context)
        val source = File(context.cacheDir, "Роман.txt").apply { writeText("Глава первая. Река медленно текла за лесом.") }
        val first = repository.importSource(contentUri(source), "Книги/Фэнтези")
        assertEquals(1, first.added)
        val book = repository.books.value.single()
        assertEquals(listOf("Книги/Фэнтези"), book.folderPaths)
        assertEquals(listOf(book), LibraryFolderTree.directBooks(repository.books.value, "Книги/Фэнтези"))
        assertTrue(LibraryFolderTree.directBooks(repository.books.value, "").isEmpty())

        val second = repository.importSource(contentUri(source), "Книги/На потом")
        assertEquals(0, second.added)
        assertEquals(1, second.linkedExisting)
        assertEquals(setOf("Книги/Фэнтези", "Книги/На потом"), repository.books.value.single().folderPaths.toSet())
        assertEquals(1, File(context.filesDir, "books").listFiles()!!.size)
    }

    @Test fun zipKeepsItsOwnHierarchyBelowTheOpenFolder() = runBlocking {
        val context = isolatedContext()
        val repository = LibraryRepository(context)
        val archive = File(context.cacheDir, "Сборник.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("Первая.txt")); zip.write("Первый рассказ. Ночь была тихой.".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("Серия/Вторая.txt")); zip.write("Второй рассказ. Утро было ясным.".toByteArray()); zip.closeEntry()
        }
        val result = repository.importSource(contentUri(archive), "Книги/Избранное")
        assertEquals(result.toString(), 2, result.added)
        assertEquals(setOf("Книги/Избранное/Сборник", "Книги/Избранное/Сборник/Серия"),
            repository.books.value.flatMap { it.folderPaths }.toSet())
        assertTrue(LibraryFolderTree.directBooks(repository.books.value, "").isEmpty())
    }

    @Test fun rootImportAndNestedFolderCreationStillWork() = runBlocking {
        val context = isolatedContext()
        val repository = LibraryRepository(context)
        val source = File(context.cacheDir, "Корень.txt").apply { writeText("Корневая книга для проверки импорта.") }
        repository.importSource(contentUri(source))
        assertEquals(1, LibraryFolderTree.directBooks(repository.books.value, "").size)
        repository.createFolder("", "Книги")
        repository.createFolder("Книги", "Фэнтези")
        assertEquals("Книги/Фэнтези/Новое", repository.createFolder("Книги/Фэнтези", "Новое"))
        assertTrue(repository.folders.value.contains("Книги/Фэнтези/Новое"))
    }

    @Test fun importedDirectoryKeepsItsRootAndSubfoldersInsideTheDestination() = runBlocking {
        val repository = LibraryRepository(isolatedContext())
        val uri = DocumentsContract.buildTreeDocumentUri("io.fluxreader.app.test.librarydocuments", "root")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(Intent().setComponent(ComponentName("io.fluxreader.app.test",
            "io.fluxreader.app.data.LibraryTestGrantActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        for (attempt in 0 until 100) {
            if (context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED) break
            Thread.sleep(20)
        }
        val result = repository.importFolder(uri, "Книги/Фэнтези")
        assertEquals(result.toString(), 2, result.added)
        assertEquals(0, result.skipped)
        assertEquals(setOf("Книги/Фэнтези/Каталог", "Книги/Фэнтези/Каталог/Серия"),
            repository.books.value.flatMap { it.folderPaths }.toSet())
        assertTrue(LibraryFolderTree.directBooks(repository.books.value, "").isEmpty())
    }

    @Test fun groupedFolderCopyMoveAndRemovalPreserveBooksAndNestedPaths() = runBlocking {
        val context = isolatedContext()
        val repository = LibraryRepository(context)
        listOf("Один", "Два").forEachIndexed { index, name ->
            val file = File(context.cacheDir, "$name.txt").apply { writeText("Разная книга $index. Тихо шелестели листья.") }
            repository.importSource(contentUri(file), "Сборник/$name/Вложенная")
        }
        repository.createFolder("", "Назначение")
        val originalIds = repository.books.value.map { it.id }.toSet()
        val paths = setOf("Сборник/Один", "Сборник/Два")
        paths.forEach { repository.copyFolder(it, "Назначение") }
        assertEquals(originalIds, repository.books.value.map { it.id }.toSet())
        assertEquals(2, File(context.filesDir, "books").listFiles()!!.size)
        paths.forEach { repository.moveFolder(it, "Назначение") }
        assertTrue(repository.books.value.all { book -> book.folderPaths.all { it.startsWith("Назначение/") } })
        setOf("Назначение/Один", "Назначение/Два").forEach { repository.deleteFolder(it) }
        assertEquals(originalIds, repository.books.value.map { it.id }.toSet())
        assertTrue(repository.books.value.all { "Назначение/Вложенная" in it.folderPaths })
        assertEquals(2, File(context.filesDir, "books").listFiles()!!.size)
    }

    private fun isolatedContext(): ContextWrapper {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "updates/library-import-tests/${System.nanoTime()}").apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
        }
    }

    private fun contentUri(file: File): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return FileProvider.getUriForFile(context, "${context.packageName}.updates.fileprovider", file)
    }
}
