package ru.happyelon.webappv2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.net.URI
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var musicService: MusicService? = null
    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(500)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true

        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        webView.webViewClient = object : MyWebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (error?.errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
                    showNoInternetScreen(savedInstanceState)
                } else {
                    super.onReceivedError(view, request, error)
                }
            }
        }

        loadWebPage(savedInstanceState)

        val client: OkHttpClient =  OkHttpClient()
        val request: Request = Request
            .Builder()
            .url("ws://185.10.68.166")
            .build()
        val listener = MySocketListener()
        val ws: WebSocket = client.newWebSocket(request, listener)
    }

    open class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()

            if (url.contains("radioulitka.ru")) {
                view.loadUrl(url)
                return false
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
                return true
            }
        }
    }


    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun showNoInternetScreen(savedInstanceState: Bundle?) {
        setContentView(R.layout.no_internet_screen)
        webView.visibility = WebView.GONE
        val retryButton: Button = findViewById(R.id.retryButton)
        retryButton.setOnClickListener {
            Log.d("BUTTON", "Button clicked")
            loadWebPage(savedInstanceState)
        }
    }

    private fun loadWebPage(savedInstanceState: Bundle?){
        if (savedInstanceState != null) {
            Log.d("LOAD_WEB_PAGE", "restoring InstanceState")
            webView.visibility = WebView.VISIBLE
            webView.restoreState(savedInstanceState)
        } else {
            Log.d("LOAD_WEB_PAGE", "loading basic url")
            webView.loadUrl("http://radioulitka.ru")
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
