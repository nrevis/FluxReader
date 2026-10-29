package io.fluxreader.app.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;

/** Acts as the system picker's grant step, for the synthetic tree only. */
public class LibraryTestGrantActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        grantUriPermission("io.fluxreader.app",
            DocumentsContract.buildTreeDocumentUri("io.fluxreader.app.test.librarydocuments", "root"),
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        finish();
    }
}
