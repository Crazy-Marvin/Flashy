package rocks.poopjournal.flashy;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import rocks.poopjournal.flashy.databinding.WelcomeBinding;


public class WelcomeActivity extends Activity {

    private TextView mTextView;
    private WelcomeBinding binding;
    private boolean isScreenOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = WelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enables Ambient mode.
//        setAmbientEnabled();
        binding.sub.setVisibility(View.VISIBLE);
        binding.main.setVisibility(View.VISIBLE);
        // Initial screen state
//        updateScreenState();

        // Toggle screen state on button click or any other event
        // For example, you can use a button click listener
        // to toggle the screen state.

        // For demonstration purposes, you can use a button click
        // listener to toggle the screen state.
        binding.main.setOnClickListener(v -> {
            isScreenOn = !isScreenOn;
            updateScreenState();
        });
    }

    private void updateScreenState() {
        if (isScreenOn) {
            binding.main.setBackgroundColor(Color.WHITE);
            binding.sub.setVisibility(View.GONE);
            // Turn on the screen by setting brightness to a non-zero value
            setScreenBrightness(255);
        } else {
            binding.main.setBackgroundColor(Color.BLACK);
            binding.sub.setVisibility(View.VISIBLE);
            // Turn off the screen by setting brightness to 0
            setScreenBrightness(0);
        }
    }

    private void setScreenBrightness(int brightness) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness / 255.0f;
        getWindow().setAttributes(layoutParams);

    }
}