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

package ac.robinson.mediaphone_gtg.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import ac.robinson.mediaphone_gtg.R;
import ac.robinson.mediaphone_gtg.provider.FrameItem;

public class PressableRelativeLayout extends RelativeLayout {

	public static final int PLAY_ICON = R.drawable.ic_menu_play;
	public static final int EDIT_ICON = R.drawable.ic_menu_settings;
	public static final int EDIT_ICON_LEFT = R.drawable.ic_narratives_insert_left;
	public static final int EDIT_ICON_RIGHT = R.drawable.ic_narratives_insert_right;

	private Integer mOverlayResource;
	private boolean mHighlightOnPress = true;

	public PressableRelativeLayout(Context context) {
		super(context);
	}

	public PressableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PressableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setPressedIcon(int resourceId) {
		mOverlayResource = null;
		// don't allow the add frame button to be overlaid with an icon
		if (resourceId > 0) {
			String frameTag = ((FrameViewHolder) getTag()).frameInternalId;
			if (!FrameItem.KEY_FRAME_ID_START.equals(frameTag) && !FrameItem.KEY_FRAME_ID_END.equals(frameTag)) {
				mOverlayResource = resourceId;
			}
		}
	}

	public void setHighlightOnPress(boolean highlightOnPress) {
		mHighlightOnPress = highlightOnPress;
	}

	@Override
	public void setPressed(boolean pressed) {
		super.setPressed(pressed);
		ImageView overlayView = (ImageView) findViewById(R.id.frame_item_action_overlay);
		if (pressed && mHighlightOnPress) {
			overlayView.setBackgroundResource(R.drawable.frame_item_highlight);
			if (mOverlayResource != null) {
				if (mOverlayResource == PLAY_ICON || mOverlayResource == EDIT_ICON) {
					overlayView.setScaleType(ScaleType.CENTER_INSIDE);
				} else {
					overlayView.setScaleType(ScaleType.CENTER_CROP);
				}
				overlayView.setImageResource(mOverlayResource);
			}
		} else {
			mOverlayResource = null;
			overlayView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			overlayView.setImageDrawable(null);
		}
	}
}
