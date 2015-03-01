package ac.robinson.mediaphone_gtg.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;

import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;
import ac.robinson.util.UIUtilities;

public class ResourceActivity extends MediaPhoneActivity {

	private static final String INJECTION_TOKEN = "**injection**";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIUtilities.configureActionBar(this, true, true, R.string.title_resources, R.string.title_resources);
		setContentView(R.layout.resource_viewer);

		final Intent intent = getIntent();
		if (intent != null) {
			int resourceId = intent.getIntExtra(getString(R.string.extra_resource_id), -1);

			WebView webView = (WebView) findViewById(R.id.resource_viewer_webview);
			webView.loadUrl(String.format("file:///android_res/raw/resource_%d.html", resourceId));
		}
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {

	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {

	}
}
