package ac.robinson.mediaphone_gtg.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.View;

import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;

public class ResourceChooserActivityRussian extends MediaPhoneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resources_chooser_russian);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {

	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {

	}

	public void handleResourceClick(View view) {
		final int resourceId;
		switch(view.getId()) {
			case R.id.resource_button_1:
				resourceId = 1;
				break;
			case R.id.resource_button_2:
				resourceId = 2;
				break;
			case R.id.resource_button_3:
				resourceId = 3;
				break;
			case R.id.resource_button_4:
				resourceId = 4;
				break;
			case R.id.resource_button_5:
				resourceId = 5;
				break;
			case R.id.resource_button_6:
				resourceId = 6;
				break;

			default:
				resourceId = 1;
				break;
		}

		Intent resourceIntent = new Intent(ResourceChooserActivityRussian.this, ResourceActivityRussian.class);
		resourceIntent.putExtra(getString(R.string.extra_resource_id), resourceId);
		startActivity(resourceIntent);
	}
}
