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

package ac.robinson.mediaphone_gtg.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.FileOutputStream;

import ac.robinson.mediaphone_gtg.MediaPhone;
import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;
import ac.robinson.mediaphone_gtg.provider.FrameItem;
import ac.robinson.mediaphone_gtg.provider.FramesManager;
import ac.robinson.mediaphone_gtg.provider.MediaItem;
import ac.robinson.mediaphone_gtg.provider.MediaManager;
import ac.robinson.mediaphone_gtg.provider.MediaPhoneProvider;
import ac.robinson.mediaphone_gtg.view.ColorPickerDialog;
import ac.robinson.mediaphone_gtg.view.MediaDurationDialog;
import ac.robinson.util.IOUtilities;
import ac.robinson.util.UIUtilities;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

public class TextActivity extends MediaPhoneActivity {

	private String mMediaItemInternalId = null;
	private boolean mHasEditedMedia = false;

	private EditText mEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_view);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mEditText = (EditText) findViewById(R.id.text_view);
		mMediaItemInternalId = null;

		// load previous state on screen rotation
		if (savedInstanceState != null) {
			mMediaItemInternalId = savedInstanceState.getString(getString(R.string.extra_internal_id));
			mHasEditedMedia = savedInstanceState.getBoolean(getString(R.string.extra_media_edited));
			if (mHasEditedMedia) {
				setBackButtonIcons(TextActivity.this, R.id.button_finished_text, 0, true);
			}
		}

		// load the media itself
		loadMediaContainer();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString(getString(R.string.extra_internal_id), mMediaItemInternalId);
		savedInstanceState.putBoolean(getString(R.string.extra_media_edited), mHasEditedMedia);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// we want to get notifications when the text is changed (but after adding existing text in onCreate)
		mEditText.addTextChangedListener(mTextWatcher);
	}

	@Override
	protected void onPause() {
		// we don't want to get the notification that the text was removed from the window on pause or destroy
		mEditText.removeTextChangedListener(mTextWatcher);
		super.onPause();
	}

	private TextWatcher mTextWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (!mHasEditedMedia) {
				mHasEditedMedia = true; // so we keep the same icon on rotation
				setBackButtonIcons(TextActivity.this, R.id.button_finished_text, 0, true);
			}
			mHasEditedMedia = true;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			mHasEditedMedia = true; // just in case
		}
	};

	@Override
	public void onBackPressed() {
		// managed to press back before loading the media - wait
		if (mMediaItemInternalId == null) {
			return;
		}

		final MediaItem textMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
		if (textMediaItem != null) {
			// check whether we need to save the text or delete the media item (if empty)
			final Editable mediaText = mEditText.getText();
			if (!TextUtils.isEmpty(mediaText)) {
				if (mHasEditedMedia) {

					// update the text duration if not user-set (note negative value)
					if (textMediaItem.getDurationMilliseconds() <= 0) {
						textMediaItem.setDurationMilliseconds(-MediaItem.getTextDurationMilliseconds(mediaText.toString()));
						MediaManager.updateMedia(getContentResolver(), textMediaItem);
					}

					Runnable textUpdateRunnable = new Runnable() {
						@Override
						public void run() {
							// save the current text
							FileOutputStream fileOutputStream = null;
							try {
								fileOutputStream = new FileOutputStream(textMediaItem.getFile());
								fileOutputStream.write(mediaText.toString().getBytes());
								// fileOutputStream.flush(); // does nothing in FileOutputStream
							} catch (Throwable t) {
								// no need to update the icon - nothing has changed
							} finally {
								IOUtilities.closeStream(fileOutputStream);
							}
						}
					};

					// update this frame's icon with the new text; propagate to following frames if applicable
					updateMediaFrameIcons(textMediaItem, textUpdateRunnable);
				}
			} else {
				// delete the media item
				textMediaItem.setDeleted(true);
				MediaManager.updateMedia(getContentResolver(), textMediaItem);

				// we've been deleted - propagate changes to our parent frame and any following frames
				inheritMediaAndDeleteItemLinks(textMediaItem.getParentId(), textMediaItem, null);
			}

			// save the id of the frame we're part of so that the frame editor gets notified
			saveLastEditedFrame(textMediaItem.getParentId());
		}

		// force hide the soft keyboard so that the layout refreshes next time we launch TODO: refresh layout?
		try {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				inputMethodManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
			}
		} catch (Throwable t) {
			// on some phones, and with custom keyboards, this fails, and crashes - catch instead
		}

		setResult(mHasEditedMedia ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
		super.onBackPressed();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			onBackPressed(); // so we go back as well as hiding the keyboard
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		createMediaMenuNavigationButtons(inflater, menu, mHasEditedMedia);
		inflater.inflate(R.menu.change_text_colour, menu);
		inflater.inflate(R.menu.set_text_duration, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		prepareMediaMenuNavigationButtons(menu, mMediaItemInternalId);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add_frame:
				final MediaItem textMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
				if (textMediaItem != null && !TextUtils.isEmpty(mEditText.getText())) {
					final String newFrameId = insertFrameAfterMedia(textMediaItem);
					final Intent addTextIntent = new Intent(TextActivity.this, TextActivity.class);
					addTextIntent.putExtra(getString(R.string.extra_parent_id), newFrameId);
					startActivity(addTextIntent);

					onBackPressed();
				} else {
					UIUtilities.showToast(TextActivity.this, R.string.split_text_add_content);
				}
				return true;

			case R.id.menu_back_without_editing:
			case R.id.menu_finished_editing:
				onBackPressed();
				return true;

			case R.id.menu_change_text_colour:
				final MediaItem colourMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
				if (colourMediaItem != null) {
					FrameItem colourChangeFrame = FramesManager.findFrameByInternalId(getContentResolver(),
							colourMediaItem.getParentId());
					int currentColour = colourChangeFrame.getForegroundColour();
					new ColorPickerDialog(TextActivity.this, mColourChangedListener, currentColour).show();
				}
				return true;

			case R.id.menu_set_text_duration:
				final MediaItem durationMediaItem = MediaManager.findMediaByInternalId(getContentResolver(),
						mMediaItemInternalId);
				if (durationMediaItem != null) {
					int currentDuration = durationMediaItem.getDurationMilliseconds();
					if (currentDuration == -1) {
						final Editable mediaText = mEditText.getText();
						if (!TextUtils.isEmpty(mediaText)) {
							currentDuration = MediaItem.getTextDurationMilliseconds(mediaText.toString());
						} else {
							TypedValue resourceValue = new TypedValue();
							getResources().getValue(R.dimen.default_minimum_frame_duration, resourceValue, true);
							float minimumFrameDuration;
							try {
								SharedPreferences mediaPhoneSettings =
										PreferenceManager.getDefaultSharedPreferences(TextActivity.this);
								minimumFrameDuration =
										mediaPhoneSettings.getFloat(getString(R.string.key_minimum_frame_duration), resourceValue
										.getFloat());
								if (minimumFrameDuration <= 0) {
									throw new NumberFormatException();
								}
							} catch (Exception e) {
								minimumFrameDuration = resourceValue.getFloat();
							}
							// min duration is in seconds; we need milliseconds
							currentDuration = Math.round(minimumFrameDuration * 1000);
						}
					} else {
						currentDuration = Math.abs(currentDuration); // calculated durations are negative
					}
					new MediaDurationDialog(TextActivity.this, mDurationSelectedListener, currentDuration).show();
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {
		// no normal preferences apply to this activity
	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {
		// the soft done/back button
		// TODO: remove this to fit with new styling (Toolbar etc)
		int newVisibility = View.VISIBLE;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ||
				!mediaPhoneSettings.getBoolean(getString(R.string.key_show_back_button),
						getResources().getBoolean(R.bool.default_show_back_button))) {
			newVisibility = View.GONE;
		}
		findViewById(R.id.button_finished_text).setVisibility(newVisibility);
	}

	private void loadMediaContainer() {

		// first launch
		ContentResolver contentResolver = getContentResolver();
		if (mMediaItemInternalId == null) {

			// editing an existing frame
			String parentInternalId = null;
			final Intent intent = getIntent();
			if (intent != null) {
				parentInternalId = intent.getStringExtra(getString(R.string.extra_parent_id));
			}
			if (parentInternalId == null) {
				UIUtilities.showToast(TextActivity.this, R.string.error_loading_text_editor);
				mMediaItemInternalId = "-1"; // so we exit
				onBackPressed();
				return;
			}

			// get existing content if it exists (ignores links)
			mMediaItemInternalId = FrameItem.getTextContentId(contentResolver, parentInternalId);

			// add a new media item if it doesn't already exist
			if (mMediaItemInternalId == null) {
				MediaItem textMediaItem = new MediaItem(parentInternalId, MediaPhone.EXTENSION_TEXT_FILE,
						MediaPhoneProvider.TYPE_TEXT);
				mMediaItemInternalId = textMediaItem.getInternalId();
				MediaManager.addMedia(contentResolver, textMediaItem);
			}
		}

		// load any existing text
		final MediaItem textMediaItem = MediaManager.findMediaByInternalId(contentResolver, mMediaItemInternalId);
		if (textMediaItem != null) {
			updateSpanFramesButtonIcon(R.id.button_toggle_mode_text, textMediaItem.getSpanFrames(), false);

			if (TextUtils.isEmpty(mEditText.getText())) { // don't delete existing (i.e. changed) content
				mEditText.setText(IOUtilities.getFileContents(textMediaItem.getFile().getAbsolutePath()));
			}
			// show the keyboard as a further hint (below Honeycomb it is automatic)
			// TODO: improve/remove these keyboard manipulations
			try {
				InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (manager != null) {
					manager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
				}
			} catch (Throwable t) {
				// on some phones this causes problems
			}
			mEditText.requestFocus();
		} else {
			UIUtilities.showToast(TextActivity.this, R.string.error_loading_text_editor);
			onBackPressed();
		}
	}

	private ColorPickerDialog.OnColorChangedListener mColourChangedListener = new ColorPickerDialog.OnColorChangedListener() {
		@Override
		public void colorChanged(int colour) {
			final MediaItem colourMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
			if (colourMediaItem != null) {
				FrameItem currentFrame = FramesManager.findFrameByInternalId(getContentResolver(),
						colourMediaItem.getParentId());
				currentFrame.setForegroundColour(colour);
				FramesManager.updateFrame(getContentResolver(), currentFrame);
				mHasEditedMedia = true;
				setBackButtonIcons(TextActivity.this, R.id.button_finished_text, 0, true);
			}
		}
	};

	private MediaDurationDialog.OnValueSelectedListener mDurationSelectedListener =
			new MediaDurationDialog.OnValueSelectedListener() {
		@Override
		public void valueSelected(int value) {
			final MediaItem durationMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
			if (durationMediaItem != null) {
				TypedValue minDuration = new TypedValue();
				getResources().getValue(R.dimen.minimum_frame_duration_min, minDuration, true);
				if (value > minDuration.getFloat() * 1000) { // attr is in seconds; we need milliseconds
					durationMediaItem.setDurationMilliseconds(value);
				} else {
					final Editable mediaText = mEditText.getText();
					if (!TextUtils.isEmpty(mediaText)) {
						durationMediaItem.setDurationMilliseconds(-MediaItem.getTextDurationMilliseconds(mediaText.toString())); // use the default calculated value when we reset
					} else {
						durationMediaItem.setDurationMilliseconds(-1);
					}
				}
				MediaManager.updateMedia(getContentResolver(), durationMediaItem);
				mHasEditedMedia = true;
				setBackButtonIcons(TextActivity.this, R.id.button_finished_text, 0, true);
			}
		}
	};

	public void handleButtonClicks(View currentButton) {
		if (!verifyButtonClick(currentButton)) {
			return;
		}

		switch (currentButton.getId()) {
			case R.id.button_finished_text:
				onBackPressed();
				break;

			case R.id.button_toggle_mode_text:
				// TODO: only relevant for text, but if they update text, set spanning, then update text again we end up
				// updating all following frame icons twice, which is unnecessary. Could track whether they've entered
				// text after toggling frame spanning, but this may be overkill for a situation that rarely happens?
				final MediaItem textMediaItem = MediaManager.findMediaByInternalId(getContentResolver(), mMediaItemInternalId);
				if (textMediaItem != null && !TextUtils.isEmpty(mEditText.getText())) {
					mHasEditedMedia = true; // so we update/inherit on exit and show the media edited icon
					setBackButtonIcons(TextActivity.this, R.id.button_finished_text, 0, true);
					boolean frameSpanning = toggleFrameSpanningMedia(textMediaItem);
					updateSpanFramesButtonIcon(R.id.button_toggle_mode_text, frameSpanning, true);
					UIUtilities.showToast(TextActivity.this, frameSpanning ? R.string.span_text_multiple_frames :
							R.string.span_text_single_frame);
				} else {
					UIUtilities.showToast(TextActivity.this, R.string.span_text_add_content);
				}
				break;

			case R.id.button_delete_text:
				final AlertDialog.Builder builder = new AlertDialog.Builder(TextActivity.this);
				builder.setTitle(R.string.delete_text_confirmation);
				builder.setMessage(R.string.delete_text_hint);
				builder.setNegativeButton(R.string.button_cancel, null);
				builder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						mEditText.setText(""); // updated & deleted in onBackPressed
						UIUtilities.showToast(TextActivity.this, R.string.delete_text_succeeded);
						onBackPressed();
					}
				});
				builder.show();
				break;

			default:
				break;
		}
	}
}
