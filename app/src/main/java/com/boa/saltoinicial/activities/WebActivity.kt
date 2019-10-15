package com.boa.saltoinicial.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.KeyEvent
import android.view.View.OnKeyListener
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.boa.saltoinicial.R.id
import com.boa.saltoinicial.R.layout
import com.boa.utils.Common
import com.boa.utils.Utils
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import io.fabric.sdk.android.Fabric
import java.util.*

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 18 oct 2017.
 */
class WebActivity : Activity() {
    lateinit var  wvAll: WebView
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(layout.activity_web)
            wvAll = findViewById<WebView?>(id.wvAll)!!
            wvAll.loadUrl(Common.WEB)
            val webSettings: WebSettings = wvAll.settings
            webSettings.javaScriptEnabled = true
            wvAll.webViewClient = MyWebClient()
            wvAll.setOnKeyListener(OnKeyListener { _, keyCode, event ->
                try {
                    //This is the filter
                    if (event.action != KeyEvent.ACTION_DOWN) {
                        return@OnKeyListener true
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (wvAll.canGoBack()) {
                            wvAll.goBack()
                        } else {
                            onBackPressed()
                        }

                        return@OnKeyListener true
                    }
                } catch (e: Exception) {
                    Utils.logError(this@WebActivity, "$localClassName:onCreate:onKey - ", e)
                }
                false
            })
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                val permissionsList: MutableList<String> = ArrayList()
                for (permission in Common.PERMISSIONS) {
                    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            permissionsList.add(permission)
                        }
                    }
                }

                if (permissionsList.isNotEmpty()) {
                    val callBack = 0
                    ActivityCompat.requestPermissions(this, Common.PERMISSIONS, callBack)
                } else {
                    init()
                }
            } else {
                init()
            }
        } catch (e: Exception) {
            Utils.logError(this, "$localClassName:onCreate - ", e)
        }
    }

    override fun onBackPressed() {
        try {
            if (wvAll.canGoBack()) {
                wvAll.goBack()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            Utils.logError(this, "$localClassName:onBackPressed - ", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        try {
            init()
        } catch (e: Exception) {
            Utils.logError(this, "$localClassName:onRequestPermissionsResult - Exception: ", e)
        }
    }

    fun init() {
        try {
            if (!Common.DEBUG) {
                Fabric.with(this, Crashlytics())
                Fabric.with(this, Answers())
            }
        } catch (e: Exception) {
            Utils.logError(this, "$localClassName:init - Exception: ", e)
        }
    }

    private inner class MyWebClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return false
        }
    }
}