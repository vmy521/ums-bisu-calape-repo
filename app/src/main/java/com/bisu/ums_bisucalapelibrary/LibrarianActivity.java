package com.bisu.ums_bisucalapelibrary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bisu.ums_bisucalapelibrary.fragment.DashboardFragment;
import com.bisu.ums_bisucalapelibrary.fragment.MonitoringFragment;
import com.bisu.ums_bisucalapelibrary.fragment.ProfilingFragment;
import com.bisu.ums_bisucalapelibrary.fragment.RecognitionFragment;
import com.bisu.ums_bisucalapelibrary.fragment.ReportFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

public class LibrarianActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private NavigationView nav_view;
    private DrawerLayout drawer;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;
    private Helper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_librarian);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.fragment_dashboard));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        auth = FirebaseAuth.getInstance();
        helper = new Helper(this);

        nav_view = findViewById(R.id.nav_view);
        drawer = findViewById(R.id.drawerLayout);
        progressDialog = new ProgressDialog(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open_drawer, R.string.nav_close_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        nav_view.setNavigationItemSelectedListener(this);

        setupUserInfo(nav_view);

        if(savedInstanceState == null){
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, new DashboardFragment());
            ft.addToBackStack(null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }
    }

    private void setupUserInfo(NavigationView nav_view) {
        //Set user info
        View view = nav_view.getHeaderView(0);
        CircleImageView civ_photo = view.findViewById(R.id.civ_photo);
        TextView tv_fullname = view.findViewById(R.id.tv_fullname);
        TextView tv_uname = view.findViewById(R.id.tv_uname);

        Glide.with(getApplicationContext())
                .load(helper.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.person)
                .into(civ_photo);
        tv_fullname.setText(helper.getFullName());
        tv_uname.setText(helper.getUsername());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LibrarianActivity.this, MyAcctActivity.class));
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        Intent intent = null;

        if(item.getItemId() == R.id.nav_logout){
            progressDialog.setTitle("Logging out");
            progressDialog.setMessage("Please wait...");
            progressDialog.show();

            logout();
            return true;
        }

        switch (item.getItemId()){
            case R.id.nav_recognition:
                toolbar.setTitle(getString(R.string.fragment_recognition));
                fragment = new RecognitionFragment();
                break;
            case R.id.nav_monitoring:
                toolbar.setTitle(getString(R.string.fragment_monitoring));
                fragment = new MonitoringFragment();
                break;
            case R.id.nav_profiling:
                toolbar.setTitle(getString(R.string.fragment_profiling));
                fragment = new ProfilingFragment();
                break;
            case R.id.nav_report:
                toolbar.setTitle(getString(R.string.fragment_report));
                fragment = new ReportFragment();
                break;
            case R.id.nav_logout:
                intent = new Intent(LibrarianActivity.this, LoginActivity.class);
                break;
            default:
                toolbar.setTitle(getString(R.string.fragment_dashboard));
                fragment = new DashboardFragment();
        }

        if(intent != null){
            startActivity(intent);
            finish();
        }else{
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.addToBackStack(null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        auth.signOut();
        helper.clearUser();
        progressDialog.dismiss();
        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        //Disable back press
    }
}