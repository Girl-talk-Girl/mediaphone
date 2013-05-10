/*
 *  Copyright (C) 2012 Simon Robinson
 * 
 *  This file is part of Com-Me.
 * 
 *  Com-Me is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 3 of the 
 *  License, or (at your option) any later version.
 *
 *  Com-Me is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General 
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with Com-Me.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ac.robinson.mediaphone.provider;

import java.util.ArrayList;
import java.util.HashMap;

import ac.robinson.mediaphone.MediaPhone;
import ac.robinson.mediautilities.FrameMediaContainer;
import ac.robinson.util.IOUtilities;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class NarrativeItem implements BaseColumns {

	public static final Uri NARRATIVE_CONTENT_URI = Uri.parse(MediaPhoneProvider.URI_PREFIX
			+ MediaPhoneProvider.URI_AUTHORITY + MediaPhoneProvider.URI_SEPARATOR
			+ MediaPhoneProvider.NARRATIVES_LOCATION);

	public static final Uri TEMPLATE_CONTENT_URI = Uri.parse(MediaPhoneProvider.URI_PREFIX
			+ MediaPhoneProvider.URI_AUTHORITY + MediaPhoneProvider.URI_SEPARATOR
			+ MediaPhoneProvider.TEMPLATES_LOCATION);

	public static final String[] PROJECTION_ALL = new String[] { NarrativeItem._ID, NarrativeItem.INTERNAL_ID,
			NarrativeItem.DATE_CREATED, NarrativeItem.SEQUENCE_ID, NarrativeItem.DELETED };

	public static final String[] PROJECTION_INTERNAL_ID = new String[] { NarrativeItem.INTERNAL_ID };

	public static final String MAX_ID = "max_id";
	public static final String[] PROJECTION_NEXT_EXTERNAL_ID = new String[] { "MAX(" + NarrativeItem.SEQUENCE_ID
			+ ") as " + MAX_ID };

	// for keeping track of the helper narrative (so we don't add multiple copies later)
	public static final String HELPER_NARRATIVE_ID = "936df7b0-72b9-11e2-bcfd-0800200c9a66"; // *DO NOT CHANGE*

	public static final String INTERNAL_ID = "internal_id";
	public static final String DATE_CREATED = "date_created";
	public static final String SEQUENCE_ID = "sequence_id";
	public static final String DELETED = "deleted";

	public static final String SELECTION_NOT_DELETED = DELETED + "=0";

	public static final String DEFAULT_SORT_ORDER = DATE_CREATED + " DESC";

	private String mInternalId;
	private long mCreationDate;
	private int mSequenceId;
	private int mDeleted;

	public NarrativeItem(String internalId, int externalId) {
		mInternalId = internalId;
		mCreationDate = System.currentTimeMillis();
		mSequenceId = externalId;
		mDeleted = 0;
	}

	public NarrativeItem(int externalId) {
		this(MediaPhoneProvider.getNewInternalId(), externalId);
	}

	public NarrativeItem() {
		this(0);
	}

	public String getInternalId() {
		return mInternalId;
	}

	public long getCreationDate() {
		return mCreationDate;
	}

	public int getSequenceId() {
		return mSequenceId;
	}

	public void setSequenceId(int sequenceId) {
		mSequenceId = sequenceId;
	}

	public boolean getDeleted() {
		return mDeleted == 0 ? false : true;
	}

	public void setDeleted(boolean deleted) {
		mDeleted = deleted ? 1 : 0;
	}

	public ArrayList<FrameMediaContainer> getContentList(ContentResolver contentResolver) {

		ArrayList<FrameMediaContainer> exportedContent = new ArrayList<FrameMediaContainer>();
		HashMap<String, Integer> longRunningAudio = new HashMap<String, Integer>(); // so we can adjust durations

		ArrayList<FrameItem> narrativeFrames = FramesManager.findFramesByParentId(contentResolver, mInternalId);
		for (FrameItem frame : narrativeFrames) {
			final String frameId = frame.getInternalId();
			ArrayList<MediaItem> frameComponents = MediaManager.findMediaByParentId(contentResolver, frameId);

			final FrameMediaContainer currentContainer = new FrameMediaContainer(frameId,
					frame.getNarrativeSequenceId());

			currentContainer.mParentId = frame.getParentId();

			for (MediaItem media : frameComponents) {
				final String mediaPath = media.getFile().getAbsolutePath();
				final int mediaType = media.getType();
				boolean spanningAudio = false;

				switch (mediaType) {
					case MediaPhoneProvider.TYPE_IMAGE_FRONT:
						currentContainer.mImageIsFrontCamera = true;
					case MediaPhoneProvider.TYPE_IMAGE_BACK:
					case MediaPhoneProvider.TYPE_VIDEO:
						currentContainer.mImagePath = mediaPath;
						break;

					case MediaPhoneProvider.TYPE_TEXT:
						currentContainer.mTextContent = IOUtilities.getFileContents(mediaPath);
						if (!TextUtils.isEmpty(currentContainer.mTextContent)) {
							currentContainer.updateFrameMaxDuration(MediaItem
									.getTextDurationMilliseconds(currentContainer.mTextContent));
						} else {
							currentContainer.mTextContent = null;
						}
						break;

					case MediaPhoneProvider.TYPE_AUDIO:
						currentContainer.addAudioFile(mediaPath, media.getDurationMilliseconds());
						spanningAudio = media.getSpanFrames();
						break;
				}

				// frame spanning images and text can just be repeated; audio needs to be split between frames
				// here we count the number of frames to split between so we can equalise later
				if (spanningAudio) {
					if (frameId.equals(media.getParentId())) {
						longRunningAudio.put(mediaPath, 1); // this is the actual parent frame
					} else {
						// this is a linked frame - increase the count
						Integer existingAudioCount = longRunningAudio.remove(mediaPath);
						if (existingAudioCount != null) {
							longRunningAudio.put(mediaPath, existingAudioCount + 1);
						}
					}
				} else {
					currentContainer.updateFrameMaxDuration(media.getDurationMilliseconds());
				}
			}

			exportedContent.add(currentContainer);
		}

		// now check all long-running audio tracks to split the audio's duration between all spanned frames
		// TODO: this doesn't really respect/control other non-spanning audio (e.g., a longer sub-track than
		// duration/count) - should decide whether it's best to split lengths equally regardless of this; adapt and pad
		// equally but leaving a longer duration for the sub-track; or, use sub-track duration as frame duration
		for (FrameMediaContainer container : exportedContent) {
			boolean longAudioFound = false;
			for (int i = 0, n = container.mAudioPaths.size(); i < n; i++) {
				final Integer audioCount = longRunningAudio.get(container.mAudioPaths.get(i));
				if (audioCount != null) {
					container.updateFrameMaxDuration(container.mAudioDurations.get(i) / audioCount);
					longAudioFound = true;
				}
			}

			// don't allow non-spanned frames to be shorter than the minimum duration
			if (!longAudioFound && container.mFrameMaxDuration <= 0) {
				container.updateFrameMaxDuration(MediaPhone.PLAYBACK_EXPORT_MINIMUM_FRAME_DURATION);
			}
		}
		return exportedContent;
	}

	public ContentValues getContentValues() {
		final ContentValues values = new ContentValues();
		values.put(INTERNAL_ID, mInternalId);
		values.put(DATE_CREATED, mCreationDate);
		values.put(SEQUENCE_ID, mSequenceId);
		values.put(DELETED, mDeleted);
		return values;
	}

	public static NarrativeItem fromCursor(Cursor c) {
		final NarrativeItem narrative = new NarrativeItem();
		narrative.mInternalId = c.getString(c.getColumnIndexOrThrow(INTERNAL_ID));
		narrative.mCreationDate = c.getLong(c.getColumnIndexOrThrow(DATE_CREATED));
		narrative.mSequenceId = c.getInt(c.getColumnIndexOrThrow(SEQUENCE_ID));
		narrative.mDeleted = c.getInt(c.getColumnIndexOrThrow(DELETED));
		return narrative;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "[" + mInternalId + "," + mCreationDate + "," + mSequenceId + "," + mDeleted
				+ "]";
	}
}
