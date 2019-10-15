package com.boa.saltoinicial.activities;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.boa.saltoinicial.R;
import com.boa.utils.Common;
import com.boa.utils.Utils;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import java.util.ArrayList;
import java.util.List;
import io.fabric.sdk.android.Fabric;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 18 oct 2017.
 */
public class WebActivity extends Activity{
	private WebView wvAll;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		try{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_web);
			wvAll = findViewById(R.id.wvAll);
			wvAll.loadUrl(Common.WEB);
			WebSettings webSettings = wvAll.getSettings();
			webSettings.setJavaScriptEnabled(true);
			wvAll.setWebViewClient(new MyWebClient());
			wvAll.setOnKeyListener(new View.OnKeyListener(){
				@Override
				public boolean onKey(View view, int keyCode, KeyEvent event){
					try{
						//This is the filter
						if(event.getAction() != KeyEvent.ACTION_DOWN){
							return true;
						}
						
						if(keyCode == KeyEvent.KEYCODE_BACK){
							if(wvAll.canGoBack()){
								wvAll.goBack();
							}else{
								onBackPressed();
							}
							
							return true;
						}
					}catch(Exception e){
						Utils.logError(WebActivity.this, getLocalClassName()+":onCreate:onKey - ", e);
					}
					
					return false;
				}
			});
			
			if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
				List<String> permissionsList = new ArrayList<>();
				
				for(String permission : Common.PERMISSIONS){
					if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
						if(!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
							permissionsList.add(permission);
						}
					}
				}
				
				String[] permissions = new String[permissionsList.size()];
				permissionsList.toArray(permissions);
				
				if(permissions.length > 0){
					int callBack = 0;
					ActivityCompat.requestPermissions(this, permissions, callBack);
				}else{
					init();
				}
			}else{
				init();
			}
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onCreate - ", e);
		}
	}
	
	@Override
	public void onBackPressed(){
		try{
			if(wvAll.canGoBack()){
				wvAll.goBack();
			}else{
				super.onBackPressed();
			}
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onBackPressed - ", e);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
		try{
			init();
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	public void init(){
		try{
			if(!Common.DEBUG){
				Fabric.with(this, new Crashlytics());
				Fabric.with(this, new Answers());
			}
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":init - Exception: ", e);
		}
	}
	
	private class MyWebClient extends WebViewClient{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url){
			return false;
		}
	}
}