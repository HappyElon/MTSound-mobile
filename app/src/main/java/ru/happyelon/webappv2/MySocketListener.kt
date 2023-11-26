package ru.happyelon.webappv2

import android.provider.Settings.Global.getString
import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MySocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("Hello World!")
        Log.d("Connection","success")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        output("Received : $text")
        //val jsonObject: JSONObject(jsonString)
        //val NowPlayingRoom = jsonObject.getString("room_id")
        //val NowPlayingSong = jsonObject.getString("song_name")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing : $code / $reason")
    }


    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Error : " + t.message+"fsda")
    }

    fun output(text: String?) {
        Log.d("MySocket", text!!)
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}