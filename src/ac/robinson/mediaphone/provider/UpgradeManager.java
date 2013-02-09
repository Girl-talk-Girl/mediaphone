package ac.robinson.mediaphone.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import ac.robinson.mediaphone.MediaPhone;
import ac.robinson.mediaphone.R;
import ac.robinson.util.DebugUtilities;
import ac.robinson.util.IOUtilities;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpgradeManager {
	public static void upgradeApplication(Context context) {
		SharedPreferences applicationVersionSettings = context.getSharedPreferences(MediaPhone.APPLICATION_NAME,
				Context.MODE_PRIVATE);
		final String versionKey = context.getString(R.string.key_application_version);
		final int currentVersion = applicationVersionSettings.getInt(versionKey, 0);

		// this is only ever for things like deleting caches and showing changes, so it doesn't really matter if we fail
		final int newVersion;
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			newVersion = info.versionCode;
		} catch (Exception e) {
			Log.d(DebugUtilities.getLogTag(context),
					"Unable to find version code - not upgrading (will try again on next launch)");
			return;
		}
		if (newVersion > currentVersion) {
			SharedPreferences.Editor prefsEditor = applicationVersionSettings.edit();
			prefsEditor.putInt(versionKey, newVersion);
			prefsEditor.apply();
		} else {
			return; // no need to upgrade - version number has not changed
		}

		// now we get the actual settings (i.e. the user's preferences) to update/query where necessary
		SharedPreferences mediaPhoneSettings = PreferenceManager.getDefaultSharedPreferences(context);
		if (currentVersion == 0) {
			// before version 15 the version code wasn't stored (and default preference values weren't set) - instead,
			// we use the number of narratives as a rough guess as to whether this is the first install or not; upgrades
			// after v15 have a version number, so will still be processed even if no narratives exist
			// TODO: one side effect of this is that upgrades from pre-v15 to the latest version will *not* perform the
			// upgrade steps if there are no narratives; for example, upgrading to v16 will not save duration prefs
			int narrativesCount = NarrativesManager.getNarrativesCount(context.getContentResolver());
			if (narrativesCount <= 0) {
				Log.d(DebugUtilities.getLogTag(context), "First install - not upgrading; installing helper narrative");
				installHelperNarrative(context);
				return;
			}
		}

		// now process the upgrades one-by-one
		Log.d(DebugUtilities.getLogTag(context), "Upgrading from version " + currentVersion + " to " + newVersion);

		// v15 changed the way icons are drawn, so they need to be re-generated
		if (currentVersion < 15) {
			MediaPhone.DIRECTORY_THUMBS = IOUtilities.getNewCachePath(context,
					MediaPhone.APPLICATION_NAME + context.getString(R.string.name_thumbs_directory), true);
		}

		// v16 updated settings screen to use sliders rather than an EditText box - must convert from string to float
		if (currentVersion < 16) {
			SharedPreferences.Editor prefsEditor = mediaPhoneSettings.edit();

			float newValue = 2.5f; // 2.5 is the default frame duration in v16 (saves reading TypedValue from prefs)
			String preferenceKey = "minimum_frame_duration"; // the old value of the frame duration key
			try {
				newValue = Float.valueOf(mediaPhoneSettings.getString(preferenceKey, Float.toString(newValue)));
			} catch (Exception e) {
			}
			prefsEditor.remove(preferenceKey);
			prefsEditor.putFloat(context.getString(R.string.key_minimum_frame_duration), newValue);

			preferenceKey = "word_duration";
			newValue = 0.2f; // 0.2 is the default frame duration in v16 (saves reading TypedValue from prefs)
			try {
				newValue = Float.valueOf(mediaPhoneSettings.getString(preferenceKey, Float.toString(newValue)));
			} catch (Exception e) {
			}
			prefsEditor.remove(preferenceKey);
			prefsEditor.putFloat(context.getString(R.string.key_word_duration), newValue);

			prefsEditor.apply();
		} // never else - we want to check every previous step every time we do this

		// TODO: remember that pre-v15 versions will not get here if no narratives exist (i.e., don't do major changes)
	}

	public static void installHelperNarrative(Context context) {
		Resources res = context.getResources();
		ContentResolver contentResolver = context.getContentResolver();
		final String narrativeId = NarrativeItem.HELPER_NARRATIVE_ID;
		final int narrativeSequenceIdIncrement = res.getInteger(R.integer.frame_narrative_sequence_increment);

		// add a narrative that gives a few tips on first use
		int[] mediaStrings = { R.string.helper_narrative_frame_1, R.string.helper_narrative_frame_2,
				R.string.helper_narrative_frame_3, R.string.helper_narrative_frame_4, };
		int[] frameImages = { 0, R.drawable.help_frame_editor, R.drawable.help_frame_export, 0 };

		for (int i = 0, n = mediaStrings.length; i < n; i++) {
			final FrameItem newFrame = new FrameItem(narrativeId, i * narrativeSequenceIdIncrement);

			// add the text
			final String frameText;
			if (i == 0) {
				frameText = String.format(context.getString(mediaStrings[i]), context.getString(R.string.app_name));
			} else if (i == n - 1) {
				frameText = String.format(context.getString(mediaStrings[i]),
						context.getString(R.string.preferences_report_problem_title),
						context.getString(R.string.title_preferences));
			} else {
				frameText = context.getString(mediaStrings[i]);
			}
			final String textUUID = MediaPhoneProvider.getNewInternalId();
			final File textContentFile = MediaItem.getFile(newFrame.getInternalId(), textUUID,
					MediaPhone.EXTENSION_TEXT_FILE);

			if (textContentFile != null) {
				try {
					FileWriter fileWriter = new FileWriter(textContentFile);
					BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
					bufferedWriter.write(frameText);
					bufferedWriter.close();
				} catch (Exception e) {
				}

				MediaItem textMediaItem = new MediaItem(textUUID, newFrame.getInternalId(),
						MediaPhone.EXTENSION_TEXT_FILE, MediaPhoneProvider.TYPE_TEXT);
				MediaManager.addMedia(contentResolver, textMediaItem);
			}

			// add the image, if applicable
			if (frameImages[i] != 0) {
				final String imageUUID = MediaPhoneProvider.getNewInternalId();
				final String imageFileExtension = "png"; // all helper images are png format
				File imageContentFile = MediaItem.getFile(newFrame.getInternalId(), imageUUID, imageFileExtension);
				FileOutputStream fileOutputStream = null;
				try {
					fileOutputStream = new FileOutputStream(imageContentFile);
					Bitmap rawBitmap = BitmapFactory.decodeResource(res, frameImages[i]);
					if (rawBitmap != null) {
						rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
						rawBitmap.recycle();
					}
				} catch (Exception e) {
				} finally {
					IOUtilities.closeStream(fileOutputStream);
				}

				if (imageContentFile.exists()) {
					MediaItem imageMediaItem = new MediaItem(imageUUID, newFrame.getInternalId(), imageFileExtension,
							MediaPhoneProvider.TYPE_IMAGE_BACK);
					MediaManager.addMedia(contentResolver, imageMediaItem);
				}
			}

			FramesManager.addFrameAndPreloadIcon(res, contentResolver, newFrame);
		}

		NarrativeItem newNarrative = new NarrativeItem(narrativeId,
				NarrativesManager.getNextNarrativeExternalId(contentResolver));
		NarrativesManager.addNarrative(contentResolver, newNarrative);
	}
}
