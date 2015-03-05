package ac.robinson.mediaphone_gtg.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;
import ac.robinson.util.UIUtilities;

public class ResourceChooserActivityEnglish extends MediaPhoneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIUtilities.configureActionBar(this, true, true, R.string.title_resources_english, R.string.title_resources_english);
		setContentView(R.layout.resources_chooser_english);
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
			case R.id.resource_button_7:
				resourceId = 7;
				break;

			default:
				resourceId = 1;
				break;
		}

		Intent resourceIntent = new Intent(ResourceChooserActivityEnglish.this, ResourceActivityEnglish.class);
		resourceIntent.putExtra(getString(R.string.extra_resource_id), resourceId);
		startActivity(resourceIntent);
	}
}
