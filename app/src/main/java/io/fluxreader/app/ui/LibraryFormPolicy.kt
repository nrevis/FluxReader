package io.fluxreader.app.ui

/** Pure validation/normalization rules shared by library forms and their tests. */
internal object LibraryFormPolicy {
    fun normalizedFolderName(value: String): String = value.trim()

    fun canSubmitFolderName(value: String): Boolean = normalizedFolderName(value).isNotBlank()

    fun normalizedBookTitle(value: String): String = value.trim()

    fun canSaveBookMetadata(title: String): Boolean = normalizedBookTitle(title).isNotBlank()
}
