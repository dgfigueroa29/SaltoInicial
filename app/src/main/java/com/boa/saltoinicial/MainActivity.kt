package com.boa.saltoinicial

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boa.saltoinicial.ui.theme.SaltoInicialTheme
import com.boa.utils.Common.WEB
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashlytics: FirebaseCrashlytics = Firebase.crashlytics
        try {
            setContent {
                SaltoInicialTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WebViewPage(WEB)
                    }
                }
            }
        } catch (e: Exception) {
            crashlytics.log("MainActivity OnCreate: ${e.message}")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(url: String) {
    val openFullDialogCustom = remember { mutableStateOf(false) }
    if (openFullDialogCustom.value) {
        // Dialog function
        Dialog(
            onDismissRequest = {
                openFullDialogCustom.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false // experimental
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth(),

                        )

                    Spacer(modifier = Modifier.height(20.dp))
                    //.........................Text: title
                    Text(
                        text = stringResource(R.string.loading),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth(),
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    //.........................Text : description
                    Text(
                        text = stringResource(R.string.please_wait),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp, start = 25.dp, end = 25.dp)
                            .fillMaxWidth(),
                        letterSpacing = 3.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    //.........................Spacer
                    Spacer(modifier = Modifier.height(24.dp))

                }

            }
        }
    }

    var backEnabled by remember { mutableStateOf(false) }
    var webView: WebView? = null
    val mutableStateTrigger = remember { mutableStateOf(false) }
    val infoDialog = remember { mutableStateOf(false) }

    //The Configuration object represents all of the current configurations, not just the ones that have changed.
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            println("landscape")
        }

        else -> {
            println("portrait")
        }
    }

    // Adding a WebView inside AndroidView
    // with layout as full screen
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()

                // to play video on a web view
                settings.javaScriptEnabled = true

                // to verify that the client requesting your web page is actually your Android app.
                settings.userAgentString =
                    System.getProperty("http.agent") //Dalvik/2.1.0 (Linux; U; Android 11; M2012K11I Build/RKQ1.201112.002)

                settings.useWideViewPort = true

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        //Retry on error
                        infoDialog.value = true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? = super.shouldInterceptRequest(view, request)

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        openFullDialogCustom.value = true
                        backEnabled = view.canGoBack()
                    }

                    // Compose WebView Part 7 | Hide elements from web view
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        openFullDialogCustom.value = false
                        removeElement(view!!)
                    }
                }

                loadUrl(url)
                webView = this
            }
        }, update = {
            webView = it
        })


    if (mutableStateTrigger.value) {
        WebViewPage(WEB)
    }

    if (infoDialog.value) {
        InfoDialog(
            title = stringResource(R.string.offline),
            desc = stringResource(R.string.offline_desc),
            onDismiss = {
                infoDialog.value = false
            }
        )
    }

    BackHandler(enabled = backEnabled) {
        removeElement(webView!!)
        webView?.goBack()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SaltoInicialTheme {
        WebViewPage(WEB)
    }
}

fun removeElement(webView: WebView) {
    // hide element by id
    webView.loadUrl("javascript:(function() { document.getElementById('blog-pager').style.display='none';})()")

    // we can also hide class name
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[0].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[1].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[2].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[3].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[4].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[5].style.display='none';})()")
    webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[6].style.display='none';})()")
}