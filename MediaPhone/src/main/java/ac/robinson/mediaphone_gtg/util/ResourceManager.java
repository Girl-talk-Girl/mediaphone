package ac.robinson.mediaphone_gtg.util;

import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ac.robinson.mediaphone_gtg.MediaPhone;
import ac.robinson.util.IOUtilities;

public class ResourceManager {

	// TODO: this search method could be made a lot more robust...
	private static final Pattern sMainContentPattern = Pattern.compile("<div id=\"mainContent\"(.*)</section></div>",
			Pattern.DOTALL);
	private static final Pattern sDateUpdatedPattern = Pattern.compile("data-updated-on=\"([0-9]+)\"");
	private static final Pattern sBlockContentPattern = Pattern.compile("<div class=\"sqs-block-content\">(.*)" +
			"</div></div></div></div></div></div>", Pattern.DOTALL);

	public static final String RESOURCE_FILE_EXTENSION = ".html"; // including the dot

	public static String getResourceName(int resourceId, String resourceLanguage) {
		return String.format("resource_%d_%s", resourceId, resourceLanguage);
	}

	public static String loadAndUpdateResource(int resourceId, String resourceLanguage, SharedPreferences preferences,
	                                           String remoteContent) {
		Matcher mainContentMatcher = sMainContentPattern.matcher(remoteContent); // get the main content block
		if (mainContentMatcher.find()) {
			String mainContent = mainContentMatcher.group(1);

			Matcher dateUpdatedMatcher = sDateUpdatedPattern.matcher(mainContent); // get the date updated
			if (dateUpdatedMatcher.find()) {
				long dateUpdated = 0;
				try {
					dateUpdated = Long.parseLong(dateUpdatedMatcher.group(1));
				} catch (NumberFormatException ignored) {
					return null;
				}

				String resourceName = getResourceName(resourceId, resourceLanguage);
				long localDateUpdated = preferences.getLong(resourceName, 0);

				if (dateUpdated > localDateUpdated) { // update if remote version is newer
					Matcher blockContentMatcher = sBlockContentPattern.matcher(mainContent);
					if (blockContentMatcher.find()) {
						String updatedContent = blockContentMatcher.group(1);
						if (saveResource(resourceName, updatedContent)) {
							SharedPreferences.Editor prefsEditor = preferences.edit();
							prefsEditor.putLong(resourceName, dateUpdated);
							prefsEditor.commit(); // apply() is better, but only in SDK >= 9
						}
						return updatedContent;
					}
				}
			}
		}
		return null;
	}

	private static boolean saveResource(String resourceName, String remoteContent) {
		File resourceFile = new File(MediaPhone.DIRECTORY_RESOURCES, resourceName + RESOURCE_FILE_EXTENSION);
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(resourceFile);
			fileOutputStream.write(remoteContent.getBytes());
			// fileOutputStream.flush(); // does nothing in FileOutputStream
			return true;
		} catch (Throwable t) {
			// not much else we can do here
			return false;
		} finally {
			IOUtilities.closeStream(fileOutputStream);
		}
	}
}
