package ac.robinson.mediaphone_gtg.view;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Adapted from the Android API samples
 */

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import ac.robinson.mediaphone_gtg.R;

public class ColorPickerDialog extends Dialog {

	public interface OnColorChangedListener {
		void colorChanged(int color);
	}

	private OnColorChangedListener mListener;
	private int mInitialColor;

	private static class ColorPickerView extends View {
		private Paint mPaint;
		private Paint mCenterPaint;
		private final int[] mColors;
		private OnColorChangedListener mListener;
		private RectF drawRect;

		ColorPickerView(Context c, OnColorChangedListener l, int color) {
			super(c);
			mListener = l;
			mColors = new int[] { 0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00,
					0xFFFFFFFF };
			Shader s = new SweepGradient(0, 0, mColors, null);

			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setShader(s);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(80);

			mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mCenterPaint.setColor(color);
			mCenterPaint.setStrokeWidth(5);

			drawRect = new RectF();
		}

		private boolean mTrackingCenter;
		private boolean mHighlightCenter;

		// no way to reuse LightingColorFilter here
		@SuppressLint("DrawAllocation")
		@Override
		protected void onDraw(Canvas canvas) {
			float r = CENTER_X - mPaint.getStrokeWidth() * 0.5f;
			drawRect.left = -r;
			drawRect.top = -r;
			drawRect.right = r;
			drawRect.bottom = r;

			canvas.translate(CENTER_X, CENTER_X);
			canvas.drawOval(drawRect, mPaint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

			if (mTrackingCenter) {
				int c = mCenterPaint.getColor();
				mCenterPaint.setStyle(Paint.Style.STROKE);

				if (mHighlightCenter) {
					mCenterPaint.setAlpha(0xFF);
				} else {
					mCenterPaint.setAlpha(0x80);
				}
				canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(), mCenterPaint);

				mCenterPaint.setStyle(Paint.Style.FILL);
				mCenterPaint.setColor(c);
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(CENTER_X * 2, CENTER_Y * 2);
		}

		private static final int CENTER_X = 200;
		private static final int CENTER_Y = 200;
		private static final int CENTER_RADIUS = 80;

		private int ave(int s, int d, float p) {
			return s + Math.round(p * (d - s));
		}

		private int interpColor(int colors[], float unit) {
			if (unit <= 0) {
				return colors[0];
			}
			if (unit >= 1) {
				return colors[colors.length - 1];
			}

			float p = unit * (colors.length - 1);
			int i = (int) p;
			p -= i;

			// now p is just the fractional part [0...1) and i is the index
			int c0 = colors[i];
			int c1 = colors[i + 1];
			int a = ave(Color.alpha(c0), Color.alpha(c1), p);
			int r = ave(Color.red(c0), Color.red(c1), p);
			int g = ave(Color.green(c0), Color.green(c1), p);
			int b = ave(Color.blue(c0), Color.blue(c1), p);

			return Color.argb(a, r, g, b);
		}

		private static final float PI = 3.1415926f;

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX() - CENTER_X;
			float y = event.getY() - CENTER_Y;
			boolean inCenter = Math.sqrt(x * x + y * y) <= CENTER_RADIUS;

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mTrackingCenter = inCenter;
					if (inCenter) {
						mHighlightCenter = true;
						invalidate();
						break;
					}
				case MotionEvent.ACTION_MOVE:
					if (mTrackingCenter) {
						if (mHighlightCenter != inCenter) {
							mHighlightCenter = inCenter;
							invalidate();
						}
					} else {
						float angle = (float) Math.atan2(y, x);
						// need to turn angle [-PI ... PI] into unit [0....1]
						float unit = angle / (2 * PI);
						if (unit < 0) {
							unit += 1;
						}
						mCenterPaint.setColor(interpColor(mColors, unit));
						invalidate();
					}
					break;
				case MotionEvent.ACTION_UP:
					playSoundEffect(SoundEffectConstants.CLICK); // play the default button click (respects prefs)
					if (mTrackingCenter) {
						if (inCenter) {
							mListener.colorChanged(mCenterPaint.getColor());
						}
						mTrackingCenter = false; // so we draw w/o halo
						invalidate();
					}
					break;
			}
			return true;
		}
	}

	public ColorPickerDialog(Context context, OnColorChangedListener listener, int initialColor) {
		super(context);

		mListener = listener;
		mInitialColor = initialColor;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OnColorChangedListener l = new OnColorChangedListener() {
			public void colorChanged(int color) {
				mListener.colorChanged(color);
				dismiss();
			}
		};

		LinearLayout mainPanel = new LinearLayout(getContext());
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		mainPanel.setPadding(0, 20, 0, 20);
		mainPanel.setGravity(Gravity.CENTER);
		mainPanel.addView(new ColorPickerView(getContext(), l, mInitialColor));

		// Button cancelButton = new Button(getContext());
		// cancelButton.setText(getContext().getString(R.string.button_cancel));
		// cancelButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// dismiss();
		// }
		// });
		// LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		// layoutParams.setMargins(0, 40, 0, 0);
		// mainPanel.addView(cancelButton, layoutParams);

		setContentView(mainPanel);
		setTitle(getContext().getString(R.string.title_choose_colour));
	}
}
