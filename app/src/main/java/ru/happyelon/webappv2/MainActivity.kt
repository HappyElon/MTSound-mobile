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
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import ru.happyelon.webappv2.MainActivity.MySocketListener.Companion.NORMAL_CLOSURE_STATUS
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var musicService: MusicService? = null
    private var isBound = false
    private var webSocket: WebSocket? = null

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

        val url = URL(webView.url)
        val protocol = url.protocol // "https"
        val ws_protocol = "ws"
        var host = url.host // "mtsound.ru"
        val path = url.path // "/path/to/resource"

        val ws_host = "$ws_protocol://$host$path"
        host = host.replace("mtsound.ru", "api.mtsound")

        val modifiedUrl = "$protocol://$host$path"

        connectWebSocket(modifiedUrl)
    }

    private fun connectWebSocket(url: String) {
        val client: OkHttpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, MySocketListener())
    }

    private class MySocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val jsonMessage = """{"type":"getRoom"}"""
            sendWebSocketMessage(jsonMessage, webSocket)
            Log.d("Connection", "success")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            output("Received : $text")

            try {
                val json = JSONObject(text)
                val name = json.getString("name")
                val track = json.getString("track")

                // Now you can use 'name' and 'track' as needed
                Log.d("WebSocket", "Name: $name, Track: $track")
            } catch (e: JSONException) {
                Log.e("WebSocket", "Error parsing JSON: $text", e)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            output("Closing : $code / $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            output("Error : " + t.message + "fsda")
        }

        private fun output(text: String?) {
            Log.d("MySocket", text!!)
        }

        private fun sendWebSocketMessage(message: String, webSocket: WebSocket) {
            webSocket.send(message)
        }

        companion object {
            private const val NORMAL_CLOSURE_STATUS = 1000
        }
    }

    open class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()

            if (url.contains("mtsound.ru")) {
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
            webView.loadUrl("https://mtsound.ru")
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

    fun getWebView(): WebView {
        return webView
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
