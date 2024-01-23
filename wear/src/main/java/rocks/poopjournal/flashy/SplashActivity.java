package rocks.poopjournal.flashy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import rocks.poopjournal.flashy.databinding.ActivitySplashBinding;


public class SplashActivity extends Activity {

    private ActivitySplashBinding binding;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        context=this;





        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            }
        }, 5000);




//        mPeriodicWorkRequest = new PeriodicWorkRequest.Builder(MyPeriodicWork.class,
//                15, TimeUnit.MINUTES)
//                .addTag("periodicWorkRequest")
//                .build();
//
//        WorkManager.getInstance().enqueue(mPeriodicWorkRequest);

    }

}