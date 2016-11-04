package ac.robinson.mediaphone_gtg.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;

import ac.robinson.mediaphone_gtg.MediaPhone;
import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;
import ac.robinson.mediaphone_gtg.util.ResourceManager;
import ac.robinson.mediaphone_gtg.util.RestClient;
import ac.robinson.util.IOUtilities;
import cz.msebera.android.httpclient.Header;

public class ResourceActivityEnglish extends MediaPhoneActivity {

	private static final String sResourceLanguageCode = "en";

	private WebView mWebView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resource_viewer);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mWebView = (WebView) findViewById(R.id.resource_viewer_webview);

		final Intent intent = getIntent();
		if (intent != null) {
			final int resourceId = intent.getIntExtra(getString(R.string.extra_resource_id), -1);

			// try to load updated remote resources
			RestClient.get(resourceId, sResourceLanguageCode, new TextHttpResponseHandler() {
				@Override
				public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
					// just fall back to local content
				}

				@Override
				public void onSuccess(int statusCode, Header[] headers, String responseString) {
					SharedPreferences preferences = getSharedPreferences(MediaPhone.APPLICATION_NAME, Context
							.MODE_PRIVATE);
					final String remoteContent = ResourceManager.loadAndUpdateResource(resourceId,
							sResourceLanguageCode, preferences, responseString);
					if (remoteContent != null) { // non-null response means content has been updated
						Snackbar.make(mWebView, R.string.resource_update_available_english, Snackbar.LENGTH_LONG)
								.setAction(R.string.resource_update_accept_english, new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								mWebView.loadDataWithBaseURL(null, remoteContent, "text/html", "utf-8", null);
							}
						}).show();
					}
				}
			});

			// load locally-cached resources
			String resourceString = null;
			String resourceName = ResourceManager.getResourceName(resourceId, sResourceLanguageCode);
			resourceString = IOUtilities.getFileContents(new File(MediaPhone.DIRECTORY_RESOURCES, resourceName +
					ResourceManager.RESOURCE_FILE_EXTENSION).getAbsolutePath());
			if (!TextUtils.isEmpty(resourceString)) {
				mWebView.loadDataWithBaseURL(null, resourceString, "text/html", "utf-8", null);
			} else {
				// load raw resources as a backup
				try {
					Resources res = getResources();
					resourceString = IOUtilities.getFileContents(res.openRawResource(res.getIdentifier(resourceName,
							"raw", getPackageName())));
				} catch (Exception ignored) {
				}

				if (!TextUtils.isEmpty(resourceString)) {
					mWebView.loadDataWithBaseURL(null, resourceString, "text/html", "utf-8", null);
				} else {
					mWebView.loadData(getString(R.string.error_loading_resource_english), "text/html", "utf-8");
				}
			}
		}
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {
	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {
	}
}
