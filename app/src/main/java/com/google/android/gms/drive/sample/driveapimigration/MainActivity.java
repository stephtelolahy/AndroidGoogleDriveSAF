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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import java.util.Collections;

/**
 * The main {@link Activity} for the Drive API migration sample app.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private static final int REQUEST_CODE_SIGN_IN = 1;
  private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

  private DriveServiceHelper mDriveServiceHelper;
  private String mOpenFileId;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Set the onClick listeners for the button bar.
    findViewById(R.id.open_btn).setOnClickListener(view -> openFilePicker());

    // Authenticate the user. For most apps, this should be done when the user performs an
    // action that requires Drive access rather than in onCreate.
    requestSignIn();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    switch (requestCode) {
      case REQUEST_CODE_SIGN_IN:
        if (resultCode == Activity.RESULT_OK && resultData != null) {
          handleSignInResult(resultData);
        }
        break;

      case REQUEST_CODE_OPEN_DOCUMENT:
        if (resultCode == Activity.RESULT_OK && resultData != null) {
          Uri uri = resultData.getData();
          if (uri != null) {
            openFileFromFilePicker(uri, null);
          }
        }
        break;
    }

    super.onActivityResult(requestCode, resultCode, resultData);
  }

  /**
   * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
   */
  private void requestSignIn() {
    GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
        .build();
    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

    // The result of the sign-in Intent is handled in onActivityResult.
    startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
  }

  /**
   * Handles the {@code result} of a completed sign-in activity initiated from {@link
   * #requestSignIn()}.
   */
  private void handleSignInResult(Intent result) {
    GoogleSignIn.getSignedInAccountFromIntent(result)
        .addOnSuccessListener(googleAccount -> {
          Toast.makeText(this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_LONG).show();

          // Use the authenticated account to sign in to the Drive service.
          GoogleAccountCredential credential =
              GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_FILE));
          credential.setSelectedAccount(googleAccount.getAccount());
          Drive googleDriveService =
              new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(
                  "Drive API Migration").build();

          // The DriveServiceHelper encapsulates all REST API and SAF functionality.
          // Its instantiation is required before handling any onClick actions.
          mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        })
        .addOnFailureListener(
            exception -> Toast.makeText(this, "Unable to sign in. " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show());
  }

  /**
   * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
   */
  private void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
  }

  /**
   * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
   * initiated by {@link #openFilePicker()}.
   */
  private void openFileFromFilePicker(Uri uri, String type) {
    /*
    if (mDriveServiceHelper != null) {
      Log.d(TAG, "Opening " + uri.getPath());

      mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri).addOnSuccessListener(nameAndContent -> {
        String name = nameAndContent.first;
        String content = nameAndContent.second;

        // Files opened through SAF cannot be modified.
        setReadOnlyMode();
      }).addOnFailureListener(exception -> Log.e(TAG, "Unable to open file from picker.", exception));
    }
    */
    Intent intent = new Intent(Intent.ACTION_VIEW);
    if (type != null) {
      intent.setDataAndType(uri, type);
    } else {
      intent.setData(uri);
    }
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Toast.makeText(this, "Cannot open file. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }
  }
}
