package com.webview.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * A single-activity WebView host.
 *
 * The website URL comes from BuildConfig.WEBSITE_URL, which is populated
 * automatically from config.properties at build time. You never edit this file.
 *
 * CAMERA IS STRIPPED OUT at multiple layers (see WebChromeClient below).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true

            // Hard-disable any camera/media capture entry points.
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
        }

        // Keep navigation inside the WebView.
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        // ----------------------------------------------------------------
        // CAMERA BLOCKING LAYER
        // ----------------------------------------------------------------
        webView.webChromeClient = object : WebChromeClient() {

            // 1) Deny every runtime permission request the page makes.
            //    This covers getUserMedia / WebRTC video (and audio) requests.
            override fun onPermissionRequest(request: PermissionRequest) {
                // Explicitly deny. We never grant VIDEO_CAPTURE (or anything else).
                request.deny()
            }

            // Belt-and-suspenders: never prompt, never auto-hide a prompt
            // in a way that could grant.
            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                // No-op: nothing was ever granted.
            }

            // 2) Block the file-chooser path. This stops
            //    <input type="file" accept="image/*" capture="camera">
            //    from being able to launch any camera capture intent.
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // Cancel the chooser outright -> no camera, no gallery capture.
                filePathCallback.onReceiveValue(null)
                return true
            }
        }

        // Inject JS as soon as pages load to make navigator.mediaDevices /
        // getUserMedia reject, so the site never even sees a usable camera API.
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view.evaluateJavascript(CAMERA_KILL_SWITCH, null)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(CAMERA_KILL_SWITCH, null)
            }
        }

        // Handle hardware back button for in-page navigation.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState == null) {
            webView.loadUrl(BuildConfig.WEBSITE_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    companion object {
        /**
         * Neutralizes camera-related web APIs on the JS side.
         * getUserMedia / enumerateDevices for video will reject or return nothing.
         */
        private const val CAMERA_KILL_SWITCH = """
            (function() {
              try {
                if (navigator.mediaDevices) {
                  navigator.mediaDevices.getUserMedia = function(constraints) {
                    return Promise.reject(new DOMException('Camera access is disabled in this app.', 'NotAllowedError'));
                  };
                  navigator.mediaDevices.enumerateDevices = function() {
                    return Promise.resolve([]);
                  };
                }
                // Legacy prefixed APIs.
                navigator.getUserMedia = navigator.webkitGetUserMedia =
                  navigator.mozGetUserMedia = navigator.msGetUserMedia = function(c, s, e) {
                    if (typeof e === 'function') e(new Error('Camera access is disabled in this app.'));
                  };
              } catch (ignored) {}
            })();
        """
    }
}
