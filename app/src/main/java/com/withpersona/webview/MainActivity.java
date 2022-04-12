package com.withpersona.webview;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

  public static final int INPUT_FILE_REQUEST_CODE = 1;
  private static final int CAMERA_PERMISSION_REQUEST = 1111;
  private WebView webView;
  private ValueCallback<Uri[]> filePathCallback;
  private String cameraPhotoPath;
  private PermissionRequest cameraPermission;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    webView = findViewById(R.id.webview);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setMediaPlaybackRequiresUserGesture(false);

    final Uri personaUrl = new Uri.Builder()
        .scheme("https")
        .encodedAuthority("inquiry.withpersona.com")
        .path("verify")
        .appendQueryParameter("is-webview", "true")
        .appendQueryParameter("inquiry-template-id", "itmpl_Ygs16MKTkA6obnF8C3Rb17dm")
        .appendQueryParameter("environment", "sandbox")
        .build();

    webView.loadUrl(personaUrl.toString());

    webView.setWebViewClient(new WebViewClient() {

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri parsedUri = Uri.parse(url);
        if (Objects.equals(parsedUri.getAuthority(), "personacallback")) {

          // User succeeded verification
          String inquiryID = parsedUri.getQueryParameter("inquiry-id");
          Toast.makeText(MainActivity.this, "The inquiry ID is " + inquiryID,
              Toast.LENGTH_SHORT)
              .show();

          // Reload Persona in the Webview
          // You will likely want to transition the view at this point.
          webView.loadUrl(personaUrl.toString());

          // Override URL loading
          return true;
        } else if (Objects.equals(parsedUri.getScheme(), "https") ||
            Objects.equals(parsedUri.getScheme(), "http")) {
          // Open in browser - this is most likely external help links
          view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
          // Override URL loading
          return true;
        } else {
          // Unknown case - do not override URL loading
          return false;
        }
      }
    });

    webView.setWebChromeClient(new WebChromeClient() {

      @Override
      public void onPermissionRequest(final PermissionRequest request) {
        if (request.getOrigin().toString().equals("https://withpersona.com/")) {
          ActivityCompat.requestPermissions(MainActivity.this,
              new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST);
          cameraPermission = request;
        } else {
          request.deny();
        }
      }

      @Override
      public boolean onShowFileChooser(
          WebView webView, ValueCallback<Uri[]> newFilePathCallback,
          FileChooserParams fileChooserParams) {

        if (filePathCallback != null) {
          filePathCallback.onReceiveValue(null);
        }
        filePathCallback = newFilePathCallback;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
          // Create the File where the photo should go
          File photoFile = null;
          // Create an image file name
          String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
          String imageFileName = "JPEG_" + timeStamp + "_";
          File storageDir =
              Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
          try {
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
          } catch (IOException ex) {
            // Error occurred while creating the File
          }

          // Continue only if the File was successfully created
          if (photoFile != null) {
            cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photoFile));
          } else {
            takePictureIntent = null;
          }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
          intentArray = new Intent[] { takePictureIntent };
        } else {
          intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

        return true;
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_PERMISSION_REQUEST) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        cameraPermission.grant(cameraPermission.getResources());
      } else {
        cameraPermission.deny();
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != INPUT_FILE_REQUEST_CODE || filePathCallback == null) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    Uri[] results = null;

    // Check that the response is a good one
    if (resultCode == Activity.RESULT_OK) {
      if (data == null) {
        // If there is not data, then we may have taken a photo
        if (cameraPhotoPath != null) {
          results = new Uri[] { Uri.parse(cameraPhotoPath) };
        }
      } else {
        String dataString = data.getDataString();
        if (dataString != null) {
          results = new Uri[] { Uri.parse(dataString) };
        }
      }
    }

    filePathCallback.onReceiveValue(results);
    filePathCallback = null;
  }
}
