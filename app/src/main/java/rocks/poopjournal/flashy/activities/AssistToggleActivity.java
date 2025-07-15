package rocks.poopjournal.flashy.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.utils.CameraHelper;

public class AssistToggleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraHelper helper = CameraHelper.getInstance(this);
        helper.toggleNormalFlash(this);  // toggle flashlight

        finish();  // immediately close activity (no UI)
    }
}