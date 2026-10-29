package io.fluxreader.app.data;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Read-only synthetic SAF tree. Java keeps the separate provider process self-contained. */
public class LibraryTestDocumentsProvider extends DocumentsProvider {
    @Override public boolean onCreate() {
        getContext().grantUriPermission("io.fluxreader.app",
            DocumentsContract.buildTreeDocumentUri("io.fluxreader.app.test.librarydocuments", "root"),
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return true;
    }
    @Override public Cursor queryRoots(String[] projection) {
        return new MatrixCursor(new String[]{"root_id", "document_id", "title"});
    }
    @Override public boolean isChildDocument(String parent, String id) { return id.startsWith(parent + "/"); }
    @Override public Cursor queryDocument(String id, String[] projection) { return rows(projection, new String[]{id}); }
    @Override public Cursor queryChildDocuments(String parent, String[] projection, String sortOrder) {
        return rows(projection, parent.equals("root") ? new String[]{"root/one.txt", "root/series"}
            : parent.equals("root/series") ? new String[]{"root/series/two.txt"} : new String[]{});
    }
    @Override public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!mode.equals("r") || !(id.equals("root/one.txt") || id.equals("root/series/two.txt"))) throw new FileNotFoundException(id);
        File file = new File(getContext().getCacheDir(), "saf-fixture-" + id.replace('/', '-'));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(("Тестовая книга " + id + ". Однажды наступило тихое утро.").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { throw new FileNotFoundException(e.getMessage()); }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    private MatrixCursor rows(String[] projection, String[] ids) {
        String[] columns = projection != null ? projection : new String[]{Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_FLAGS};
        MatrixCursor cursor = new MatrixCursor(columns);
        for (String id : ids) {
            boolean directory = id.equals("root") || id.equals("root/series");
            MatrixCursor.RowBuilder row = cursor.newRow();
            for (String column : columns) {
                Object value = null;
                if (column.equals(Document.COLUMN_DOCUMENT_ID)) value = id;
                else if (column.equals(Document.COLUMN_DISPLAY_NAME)) value = id.equals("root") ? "Каталог"
                    : id.equals("root/series") ? "Серия" : id.equals("root/one.txt") ? "Первая.txt" : "Вторая.txt";
                else if (column.equals(Document.COLUMN_MIME_TYPE)) value = directory ? Document.MIME_TYPE_DIR : "text/plain";
                else if (column.equals(Document.COLUMN_SIZE)) value = directory ? 0L : 100L;
                else if (column.equals(Document.COLUMN_FLAGS)) value = 0;
                row.add(value);
            }
        }
        return cursor;
    }
}
