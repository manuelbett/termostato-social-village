package it.termostato.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Client API che usa la WebView (rete Chromium) per le chiamate HTTP.
 * Risolve il problema di MIUI che abortisce le connessioni Java dopo ~5s.
 */
object WebViewApiClient {

    private const val TAG = "TermostatoAPI"
    private const val BASE_URL = "https://www.radiaxweb.rehom.it/EmptyApp13Web/rest/vc_service"
    private const val TIMEOUT_MS = 60_000L

    private var webView: WebView? = null
    private var isReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentCallback: ((String) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun init(context: Context) {
        if (webView != null) return
        mainHandler.post {
            webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "WebView ready at $url")
                        isReady = true
                    }
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onResult(json: String) {
                        Log.d(TAG, "WV result: ${json.take(200)}")
                        currentCallback?.invoke(json)
                        currentCallback = null
                    }
                }, "AndroidBridge")
                // Carica pagina LEGGERA dalla stessa origine (niente jQuery Mobile!)
                // Questo evita l'ANR causato dal download di 100KB+ di pagine web
                loadDataWithBaseURL(
                    "https://www.radiaxweb.rehom.it/EmptyApp13Web/",
                    "<html><body>ok</body></html>",
                    "text/html", "UTF-8", null
                )
            }
        }
    }

    suspend fun get(path: String): JSONObject {
        // Aspetta che la WebView sia pronta (la pagina leggera carica in ~100ms)
        var wait = 0
        while (!isReady && wait < 5000) {
            delay(100)
            wait += 100
        }
        if (!isReady) {
            Log.e(TAG, "WV not ready after ${wait}ms")
            return JSONObject().put("error", 6)
        }

        val urlString = "$BASE_URL/$path"
        Log.d(TAG, "WV GET $urlString")

        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine<String> { cont ->
                currentCallback = { json ->
                    if (cont.isActive) cont.resume(json)
                }
                mainHandler.post {
                    // Usa XMLHttpRequest — gestisce meglio le connessioni lente
                    val js = """
                        (function() {
                            var xhr = new XMLHttpRequest();
                            xhr.open('GET', '$urlString', true);
                            xhr.timeout = 55000;
                            xhr.onload = function() {
                                AndroidBridge.onResult(xhr.responseText);
                            };
                            xhr.onerror = function() {
                                AndroidBridge.onResult(JSON.stringify({error: 6, message: 'XHR error ' + xhr.status}));
                            };
                            xhr.ontimeout = function() {
                                AndroidBridge.onResult(JSON.stringify({error: 6, message: 'XHR timeout'}));
                            };
                            xhr.send();
                        })();
                    """.trimIndent()
                    webView?.evaluateJavascript(js, null)
                }
            }
        }

        return if (result != null) {
            try {
                JSONObject(result)
            } catch (e: Exception) {
                Log.e(TAG, "WV parse error: ${result.take(200)}")
                JSONObject().put("error", 6)
            }
        } else {
            Log.e(TAG, "WV timeout for $urlString")
            JSONObject().put("error", 6)
        }
    }
}
