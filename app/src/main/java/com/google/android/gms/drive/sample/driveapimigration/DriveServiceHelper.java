/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.drive.sample.driveapimigration;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.util.Pair;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
  private final Executor mExecutor = Executors.newSingleThreadExecutor();
  private final Drive mDriveService;

  public DriveServiceHelper(Drive driveService) {
    mDriveService = driveService;
  }

  /**
   * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
   */
  public Task<Pair<String, String>> openFileUsingStorageAccessFramework(ContentResolver contentResolver, Uri uri) {
    return Tasks.call(mExecutor, () -> {
      // Retrieve the document's display name from its metadata.
      String name;
      try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
          name = cursor.getString(nameIndex);
        } else {
          throw new IOException("Empty cursor returned for file.");
        }
      }

      // Read the document's contents as a String.
      String content;
      try (InputStream is = contentResolver.openInputStream(uri);
           BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          stringBuilder.append(line);
        }
        content = stringBuilder.toString();
      }

      return Pair.create(name, content);
    });
  }
}
