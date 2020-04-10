package com.poopjournal.flashy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import me.tankery.lib.circularseekbar.CircularSeekBar;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

public class MainActivity extends AppCompatActivity implements Camera.AutoFocusCallback {

    //Views
    CircularSeekBar seekBar;
    RelativeLayout bg_options, bg_option_circle;
    ImageView iconFlash, iconScreen, powerCenter, powerIconCenter, powerIconCenterStand;
    Snackbar flashPermission = null;
    Snackbar screenPermissions = null;
    Dialog FlashDialog = null;
    //Fields
    boolean optionSentinel = false;
    private int brightness = -999;
    private int defaultOption =1;
    private ContentResolver cResolver;
    private Window window;
    private static Camera camera = null;// has to be static, otherwise onDestroy() destroys it
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);


        findViews();
        applyListeners();






        if (savedInstanceState!=null){
            initSaved();
            int defaultOption = savedInstanceState.getInt("defaultOption");
            if (defaultOption==1){
                Log.d("flashy_test", "saved 1");
                if (savedInstanceState.getBoolean("flash")){
                    refreshActivityForFlashLight(true);
                }
            }

            if (defaultOption==2){
                Log.d("flashy_test", "saved 2, " +savedInstanceState.getInt("brightness"));
                refreshActivityForScreenLight(savedInstanceState.getInt("brightness"));
            }
        }
        else{
            init();
        }










    }


    void applyListeners(){
        bg_options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (defaultOption==1){

                    //Current Option is Flash, changing to screen
                    SharedPreferences.Editor editor = getSharedPreferences("my_prefs", MODE_PRIVATE).edit();
                    editor.putInt("default_option", 2);
                    editor.commit();
                    init();

                }
                else {
                    SharedPreferences.Editor editor = getSharedPreferences("my_prefs", MODE_PRIVATE).edit();
                    editor.putInt("default_option", 1);
                    editor.commit();
                    init();

                }

            }
        });
    }
    void init(){
        cResolver = getContentResolver();
        window = getWindow();

        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        defaultOption = prefs.getInt("default_option", 1);// 1 means flash light is selected



        if (defaultOption==1){
            if (screenPermissions!=null){
                if (screenPermissions.isShown())
                    screenPermissions.dismiss();

            }
            updateOptionsUI(true);
            refreshActivityForFlashLight();
        }
        else {
            updateOptionsUI(false);
            if(checkSystemWritePermission()){
                refreshActivityForScreenLight();
            }
          else {
              if (flashPermission!=null){
                  if (flashPermission.isShown())
                      flashPermission.dismiss();
              }
                screenPermissions = Snackbar.make(findViewById(android.R.id.content), "Need system permission to access Screen brightness", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OPEN SETTINGS", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openAndroidPermissionsMenu();
                            }
                        })
                        .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ));
                screenPermissions.show();

            }

        }


    }
    void initSaved(){
        cResolver = getContentResolver();
        window = getWindow();

//        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
//        defaultOption = prefs.getInt("default_option", 1);// 1 means flash light is selected



        if (defaultOption==1){
            if (screenPermissions!=null){
                if (screenPermissions.isShown())
                    screenPermissions.dismiss();

            }
            updateOptionsUI(true);
            refreshActivityForFlashLight();
        }
        else {
            updateOptionsUI(false);
            if(checkSystemWritePermission()){

            }
            else {
                if (flashPermission!=null){
                    if (flashPermission.isShown())
                        flashPermission.dismiss();
                }
                screenPermissions = Snackbar.make(findViewById(android.R.id.content), "Need system permission to access Screen brightness", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OPEN SETTINGS", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openAndroidPermissionsMenu();
                            }
                        })
                        .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ));
                screenPermissions.show();

            }

        }


    }
    void refreshActivityForFlashLight(){
        //Refresh Seekbar
        seekBar.setOnSeekBarChangeListener(null);
        seekBar.setProgress(0F);
        seekBar.setEnabled(false);
        seekBar.setPointerColor(Color.parseColor("#AAAABB"));
        changePowerButtonColors(false);
        //turnOff();
        boolean hasFlash = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash){
            FlashDialog = DialogsUtil.showNoFlashLightDialog(this);
            powerCenter.setOnClickListener(null);
            RelativeLayout useScreen = FlashDialog.findViewById(R.id.container_use_screen);

            useScreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FlashDialog.dismiss();
                    bg_options.callOnClick();

                }
            });
            FlashDialog.show();
            Log.d("flashy_dial","showing for simple");

            return;
        }
        else {
            powerCenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (camera==null){



                        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            flashPermission = Snackbar.make(findViewById(android.R.id.content), "Need camera permission to access Flash", Snackbar.LENGTH_LONG)
                                    .setAction("OPEN SETTINGS", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package",getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        }
                                    })
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ));
                            flashPermission.show();
                        }
                        else {
                            turnOn();
                        }


                    }
                    else{

                        turnOff();
                    }


                }
            });
        }









    }

    void refreshActivityForFlashLight(boolean turnedOn){


        //turnOff();
        //Refresh Seekbar
        seekBar.setOnSeekBarChangeListener(null);
        seekBar.setProgress(0F);
        seekBar.setEnabled(false);
        seekBar.setPointerColor(Color.parseColor("#AAAABB"));
        changePowerButtonColors(false);
        boolean hasFlash = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash){
            powerCenter.setOnClickListener(null);
           FlashDialog = DialogsUtil.showNoFlashLightDialog(this);
           RelativeLayout useScreen = FlashDialog.findViewById(R.id.container_use_screen);

           useScreen.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   FlashDialog.dismiss();
                   bg_options.callOnClick();

               }
           });

           Log.d("flashy_dial","showing for boolean");
           FlashDialog.show();

           return;
        }
        else {
            powerCenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (camera==null){



                        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            flashPermission = Snackbar.make(findViewById(android.R.id.content), "Need camera permission to access Flash", Snackbar.LENGTH_LONG)
                                    .setAction("OPEN SETTINGS", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package",getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        }
                                    })
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ));
                            flashPermission.show();
                        }
                        else {
                            turnOn();
                        }


                    }
                    else{

                        turnOff();
                    }


                }
            });
        }

    }
    void changePowerButtonColors(boolean isTurnedOn){
        if (isTurnedOn){
            powerCenter.setColorFilter(Color.parseColor("#28FFB137"));
            powerIconCenter.setColorFilter(Color.parseColor("#FFB137"));
            powerIconCenterStand.setColorFilter(Color.parseColor("#FFB137"));

        }
        else {
            //Refresh Power Button
            powerCenter.setColorFilter(Color.parseColor("#F3F3F7"));
            powerIconCenter.setColorFilter(Color.parseColor("#AAAABB"));
            powerIconCenterStand.setColorFilter(Color.parseColor("#AAAABB"));


        }
    }

    void updateOptionsUI(boolean isFlash){

        if(isFlash){
            //Change UI for options
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bg_option_circle.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            bg_option_circle.setLayoutParams(params);
            iconFlash.setColorFilter(Color.parseColor("#FFB137"));
            iconScreen.setColorFilter(Color.parseColor("#AAAABB"));

            seekBar.setProgress(0f);
//            seekBar.setCircleProgressColor(Color.parseColor("#F3F3F7"));
//            seekBar.setPointerColor(Color.parseColor("#F3F3F7"));

        }
        else {
            ((ViewGroup) bg_option_circle).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bg_option_circle.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            bg_option_circle.setLayoutParams(params);
            iconFlash.setColorFilter(Color.parseColor("#AAAABB"));
            iconScreen.setColorFilter(Color.parseColor("#FFB137"));
//            seekBar.setCircleProgressColor(Color.parseColor("#FFB137"))
//            ;
//            seekBar.setPointerColor(Color.parseColor("#FFB137"));
        }

    }
    void refreshActivityForScreenLight(){


        seekBar.setPointerColor(Color.parseColor("#FFB137"));

        seekBar.setEnabled(true);
        powerCenter.setOnClickListener(null);
        changePowerButtonColors(false);
        turnOff();

        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                brightness = (int) progress;

                WindowManager.LayoutParams layoutpars = window.getAttributes();
                layoutpars.screenBrightness = brightness / (float)100;
                window.setAttributes(layoutpars);

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        powerCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (brightness==0){
                    brightness = 100;
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    layoutpars.screenBrightness = brightness / (float)100;
                    window.setAttributes(layoutpars);
                    seekBar.setProgress(100);
                }
                else {
                    brightness = 0;
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    layoutpars.screenBrightness = brightness / (float)100;
                    window.setAttributes(layoutpars);
                    seekBar.setProgress(0);
                }
            }
        });


    }

    void refreshActivityForScreenLight(int brightnessToCurrentlyUse){

        seekBar.setPointerColor(Color.parseColor("#FFB137"));


        seekBar.setEnabled(true);
        powerCenter.setOnClickListener(null);

        changePowerButtonColors(false);
        turnOff();
        brightness = (int) brightnessToCurrentlyUse;

        WindowManager.LayoutParams layoutpars = window.getAttributes();
        layoutpars.screenBrightness = brightness / (float)100;
        window.setAttributes(layoutpars);
        float currentBrightness = brightness/(float) 255;
        seekBar.setProgress(currentBrightness*100);

        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                brightness = (int) progress;

                WindowManager.LayoutParams layoutpars = window.getAttributes();
                layoutpars.screenBrightness = brightness / (float)100;
                window.setAttributes(layoutpars);


            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        powerCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (brightness==0){
                    brightness = 100;
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    layoutpars.screenBrightness = brightness / (float)100;
                    window.setAttributes(layoutpars);
                    seekBar.setProgress(100);
                }
                else {
                    brightness = 0;
                    WindowManager.LayoutParams layoutpars = window.getAttributes();
                    layoutpars.screenBrightness = brightness / (float)100;
                    window.setAttributes(layoutpars);
                    seekBar.setProgress(0);
                }
            }
        });

    }
    void findViews(){
        seekBar = findViewById(R.id.progress_circular);
        bg_options = findViewById(R.id.bg_options);
        bg_option_circle = findViewById(R.id.bg_option_circle);
        iconFlash = findViewById(R.id.flash_icon);
        iconScreen = findViewById(R.id.screen_icon);
        powerCenter = findViewById(R.id.power_center);
        powerIconCenter = findViewById(R.id.power_icon_center);
        powerIconCenterStand = findViewById(R.id.power_icon_center_stand);
    }
    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
          //  Log.d("flashy_test", "Can Write Settings: " + retVal);

        }
        return retVal;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    public void turnOn() {


        camera = Camera.open();
        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(getFlashOnParameter());
            camera.setParameters(parameters);

            camera.setPreviewTexture(new SurfaceTexture(0));

            camera.startPreview();
            camera.autoFocus(this);
            changePowerButtonColors(true);

        } catch (Exception e) {
            // We are expecting this to happen on devices that don't support autofocus.
        }

    }
    private String getFlashOnParameter() {
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();

        if (flashModes.contains(FLASH_MODE_TORCH)) {
            return FLASH_MODE_TORCH;
        } else if (flashModes.contains(FLASH_MODE_ON)) {
            return FLASH_MODE_ON;
        } else if (flashModes.contains(FLASH_MODE_AUTO)) {
            return FLASH_MODE_AUTO;
        }
        throw new RuntimeException();
    }
    public void turnOff() {
        try {
            camera.stopPreview();
            camera.release();
            camera = null;
            changePowerButtonColors(false);
        } catch (Exception e) {
            // This will happen if the camera fails to turn on.
        }
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        defaultOption = prefs.getInt("default_option", 1);// 1 means flash light is selected



        if (defaultOption==1){  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);

            List<String> permissions = new ArrayList<String>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);

            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }}



    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 111: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("Permissions --> " + "Permission Granted: " + permissions[i]);


                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        System.out.println("Permissions --> " + "Permission Denied: " + permissions[i]);
                        /*if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Need permissions", Snackbar.LENGTH_LONG);
                            snackbar.setAction("Settings", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            });
                            snackbar.show();
                        }*/
                    }

                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        if (FlashDialog!=null){
            if (FlashDialog.isShowing()){
                FlashDialog.dismiss();
                refreshActivityForScreenLight();
            }
        }
        if (defaultOption==1){
            outState.putInt("defaultOption",defaultOption);
            if (camera!=null){
                outState.putBoolean("flash",true);
            }
            Log.d("flashy_test", "1");
        }

        if (defaultOption==2){
            outState.putInt("defaultOption",defaultOption);
            outState.putInt("brightness",brightness);
            Log.d("flashy_test", "2");
        }
    }


   /* @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (camera!=null)
            turnOn();

        if (defaultOption==2){
            refreshActivityForScreenLight(brightness);
        }
    }*/


}
