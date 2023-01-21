package com.bisu.ums_bisucalapelibrary;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ImageView iv_logo;
    private TextView tv_ums, tv_library, tv_version;
    private Animation to_top_anim, to_bottom_anim, to_left_anim, to_right_anim;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        iv_logo = findViewById(R.id.iv_logo);
        tv_ums = findViewById(R.id.tv_ums);
        tv_library = findViewById(R.id.tv_library);
        tv_version = findViewById(R.id.tv_version);
        to_top_anim = AnimationUtils.loadAnimation(this, R.anim.to_top_anim);
        to_bottom_anim = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);
        to_left_anim = AnimationUtils.loadAnimation(this, R.anim.to_left_anim);
        to_right_anim = AnimationUtils.loadAnimation(this, R.anim.to_right_anim);

        //Set app version
        tv_version.setText("Version " + BuildConfig.VERSION_NAME);

        //Set animation
        iv_logo.setAnimation(to_bottom_anim);
        tv_ums.setAnimation(to_right_anim);
        tv_library.setAnimation(to_left_anim);
        tv_version.setAnimation(to_top_anim);

        //Move to next screen
        moveToNextScreen();
    }

    private void moveToNextScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;

                //Check if user already signed-in
                if(auth.getCurrentUser() != null){
                    intent = new Intent(MainActivity.this, LibrarianActivity.class);
                }else{
                    intent = new Intent(MainActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish();
            }
        }, 5000);
    }

}