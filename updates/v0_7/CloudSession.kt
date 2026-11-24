package com.mlbb.assistant

import android.os.SystemClock
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection

/** Only an operator-configured HTTPS endpoint. No cloud API keys in APK. */
object CloudSession {
    private var retryAt=0L
    @Synchronized fun connect(base:String):String {
        require(base.startsWith("https://")){"HTTPS obrigatório"}
        if(SystemClock.elapsedRealtime()<retryAt)throw IllegalStateException("Aguardando reconexão")
        retryAt=SystemClock.elapsedRealtime()+60_000
        val health=call(base.trimEnd('/')+"/health","GET")
        if(health.optString("vision")!="online")throw IllegalStateException("Visão online indisponível")
        val token=call(base.trimEnd('/')+"/session","POST").optString("token")
        require(token.isNotBlank()){"Sessão não provisionada"}
        retryAt=0
        return token
    }
    private fun call(url:String,method:String):JSONObject{
        val c=URL(url).openConnection() as HttpURLConnection
        try{
            c.instanceFollowRedirects=false;c.requestMethod=method;c.connectTimeout=6000;c.readTimeout=6000
            c.setRequestProperty("Accept","application/json")
            if(method=="POST"){c.doOutput=true;c.setFixedLengthStreamingMode(0);c.outputStream.close()}
            if(c.responseCode !in 200..299)throw IllegalStateException("Conexão HTTP "+c.responseCode)
            return JSONObject(c.inputStream.bufferedReader().use{it.readText()})
        }finally{c.disconnect()}
    }
}
