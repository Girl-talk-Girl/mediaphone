package ac.robinson.mediaphone_gtg.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;

import ac.robinson.mediaphone_gtg.R;

// NOTE: this is a very hacky rough dialog to be improved for the final version
public class MediaDurationDialog extends Dialog implements SeekBar.OnSeekBarChangeListener {

	public interface OnValueSelectedListener {
		void valueSelected(int value);
	}

	private SeekBar mSeekBar;
	private OnValueSelectedListener mListener;
	private int mInitialValue;
	private boolean mHasEditedValue;

	public MediaDurationDialog(Context context, OnValueSelectedListener listener, int currentDuration) {
		super(context);

		mListener = listener;
		mInitialValue = currentDuration;
		mHasEditedValue = false;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mHasEditedValue = true;
		setTitle(getContext().getString(R.string.title_set_duration_value, progress + 1)); // minimum is 1 second
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LinearLayout mainPanel = new LinearLayout(getContext());
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		mainPanel.setPadding(0, 20, 0, 0);
		mainPanel.setGravity(Gravity.CENTER);

		mSeekBar = new SeekBar(getContext());
		mSeekBar.setPadding(25, 20, 25, 20);
		mSeekBar.setProgress(Math.round(mInitialValue / 1000f) - 1);
		mSeekBar.setMax(59);
		mSeekBar.setOnSeekBarChangeListener(MediaDurationDialog.this);
		mainPanel.addView(mSeekBar);

		LinearLayout buttonLayout = new LinearLayout(getContext());
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		LayoutParams buttonParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f);

		Button resetButton = new Button(getContext());
		resetButton.setText(getContext().getString(R.string.button_reset));
		resetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.valueSelected(-1);
				dismiss();
			}
		});

		Button acceptButton = new Button(getContext());
		acceptButton.setText(getContext().getString(R.string.button_save));
		acceptButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mHasEditedValue) {
					mListener.valueSelected((mSeekBar.getProgress() + 1) * 1000);
				}
				dismiss();
			}
		});

		buttonLayout.addView(resetButton, buttonParams);
		buttonLayout.addView(acceptButton, buttonParams);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mainPanel.addView(buttonLayout, layoutParams);

		setContentView(mainPanel);
		if (mInitialValue > 0) {
			setTitle(getContext().getString(R.string.title_set_duration_value, mSeekBar.getProgress() + 1));
		} else {
			setTitle(getContext().getString(R.string.title_set_duration));
		}
	}
}
