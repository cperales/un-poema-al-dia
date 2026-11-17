package com.webpoema.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pageReady = false
    private var pendingPoem: Poem? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled by the system */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()
        reconcileSchedule()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Serve assets over https://appassets.androidplatform.net/ so that
        // fetch('poems.json') works (it is blocked on file:// URLs).
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                pendingPoem?.let {
                    pendingPoem = null
                    injectPoem(it)
                }
            }
        }
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        showPoemForIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showPoemForIntent(intent)
    }

    /**
     * The notification carries the poem it showed; otherwise load the shared
     * poem of the day. The WebView already displays a bundled poem meanwhile.
     */
    private fun showPoemForIntent(intent: Intent?) {
        val title = intent?.getStringExtra("title")
        val author = intent?.getStringExtra("author")
        val text = intent?.getStringExtra("text")
        val show = intent?.getBooleanExtra("show_poem", false) == true

        if (
            show &&
            !title.isNullOrBlank() &&
            !text.isNullOrBlank()
        ) {
            displayPoem(Poem(title, author ?: "", text))
            return
        }

        TodayRepository.executor.execute {
            val poem = TodayRepository.fetchBlocking(applicationContext) ?: return@execute
            runOnUiThread {
                if (!isFinishing && !isDestroyed) displayPoem(poem)
            }
        }
    }

    private fun displayPoem(poem: Poem) {
        if (pageReady) {
            injectPoem(poem)
        } else {
            pendingPoem = poem
        }
    }

    private fun injectPoem(poem: Poem) {
        val json = JSONObject().apply {
            put("title", poem.title)
            put("author", poem.author)
            put("text", poem.text)
        }
        webView.evaluateJavascript("window.showPoem && window.showPoem(($json));", null)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Normalize the alarm after app updates/reboots (it might be at an old time). */
    private fun reconcileSchedule() {
        NotificationScheduler.schedule(this, PreferenceManager.getFrequency(this))
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun openSettings() {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
    }
}
