package ac.robinson.mediaphone_gtg.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.WebView;

import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;
import ac.robinson.util.IOUtilities;
import ac.robinson.util.UIUtilities;

public class ResourceActivityRussian extends MediaPhoneActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIUtilities.configureActionBar(this, true, true, R.string.title_resources_russian, R.string.title_resources_russian);
		setContentView(R.layout.resource_viewer);

		final Intent intent = getIntent();
		if (intent != null) {
			int resourceId = intent.getIntExtra(getString(R.string.extra_resource_id), -1);

			String resourceString = null;
			try {
				Resources res = getResources();
				resourceString = IOUtilities.getFileContents(res.openRawResource(res.getIdentifier(String.format
						("resource_%d_ru", resourceId), "raw", getPackageName())));
			} catch (Exception e) {
			}

			WebView webView = (WebView) findViewById(R.id.resource_viewer_webview);
			if (resourceString != null) {
				webView.loadDataWithBaseURL(null, resourceString, "text/html", "utf-8", null);
			} else {
				webView.loadData(getString(R.string.error_loading_resource_russian), "text/html", "utf-8");
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
