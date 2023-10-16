package com.withpersona.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.withpersona.demo.webview.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val INPUT_FILE_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION_REQUEST = 1111
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null
    private var cameraPermission: PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Allows debugging via Chrome at chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true)

        val webView = findViewById<WebView>(R.id.webview)

        with(webView.settings) {
            domStorageEnabled = true
            @Suppress("SetJavaScriptEnabled")
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        val personaUrl = Uri.Builder()
            .scheme("https")
            .encodedAuthority("inquiry.withpersona.com")
            .path("verify")
            .appendQueryParameter("is-webview", "true")
            .appendQueryParameter("inquiry-template-id", "itmpl_Ygs16MKTkA6obnF8C3Rb17dm")
            .appendQueryParameter("environment", "sandbox")
            .build()

        webView.loadUrl(personaUrl.toString())

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val parsedUri = Uri.parse(url)
                return if (parsedUri.authority == "personacallback") {

                    // User succeeded verification
                    val inquiryID = parsedUri.getQueryParameter("inquiry-id")
                    Toast.makeText(
                        this@MainActivity, "The inquiry ID is $inquiryID",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    // Reload Persona in the Webview
                    // You will likely want to transition the view at this point.
                    webView.loadUrl(personaUrl.toString())

                    // Override URL loading
                    true
                } else if (parsedUri.scheme == "https" || parsedUri.scheme == "http") {
                    // Open in browser - this is most likely external help links
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    // Override URL loading
                    true
                } else {
                    // Unknown case - do not override URL loading
                    false
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (request.origin.toString() == "https://inquiry.withpersona.com/") {
                     ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST
                    )
                    cameraPermission = request
                } else {
                    request.deny()
                }
            }

            override fun onShowFileChooser(
                webView: WebView, newFilePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = newFilePathCallback

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent?.resolveActivity(packageManager) != null) {
                    // Create the File where the photo should go
                    var photoFile: File? = null
                    // Create an image file name
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val imageFileName = "JPEG_" + timeStamp + "_"
                    val storageDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    try {
                        photoFile = File.createTempFile(imageFileName, ".jpg", storageDir)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        cameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                } else {
                    takePictureIntent = null
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }

                val intentArray: Array<Intent?> = takePictureIntent?.let { arrayOf(it) }
                    ?: arrayOfNulls(0)

                val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                    putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                }

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraPermission?.grant(cameraPermission!!.resources)
            } else {
                cameraPermission?.deny()
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || filePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (cameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(cameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }
}