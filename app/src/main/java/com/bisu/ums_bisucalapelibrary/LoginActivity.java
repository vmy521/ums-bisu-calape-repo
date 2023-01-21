package com.bisu.ums_bisucalapelibrary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bisu.ums_bisucalapelibrary.model.Librarian;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private TextInputLayout uname_layout, pword_layout;
    private TextView tv_forgot_pword;
    private Button login_btn;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Helper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        helper = new Helper(this);

        uname_layout = findViewById(R.id.uname_layout);
        pword_layout = findViewById(R.id.pword_layout);
        tv_forgot_pword = findViewById(R.id.tv_forgot_pword);
        login_btn = findViewById(R.id.login_btn);
        progressDialog = new ProgressDialog(this);

        login_btn.setOnClickListener(this);
        tv_forgot_pword.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login_btn:
                login();
                break;
            case R.id.tv_forgot_pword:
                startActivity(new Intent(this, ForgotPwordActivity.class));
                break;
        }
    }

    private void login() {
        String uname = uname_layout.getEditText().getText().toString().trim();
        String pword = pword_layout.getEditText().getText().toString().trim();

        if(!validateUname(uname) | !validatePword(pword)){
            return;
        }

        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        //Check if username exist
        Query query = db.collection("Librarian").whereEqualTo("username", uname);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.isSuccessful()){
                    progressDialog.hide();
                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(task.getResult().isEmpty()){
                    progressDialog.hide();
                    uname_layout.setErrorEnabled(true);
                    uname_layout.setError("Username not found");
                    return;
                }

                uname_layout.setErrorEnabled(false);
                uname_layout.setError(null);

                DocumentSnapshot document = task.getResult().iterator().next();
                Librarian librarian = document.toObject(Librarian.class);

                //Sign in to firebaseAuth
                auth.signInWithEmailAndPassword(librarian.getEmail(), pword)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(!task.isSuccessful()){
                                    progressDialog.hide();
                                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                progressDialog.dismiss();

                                //Save user to sharedPreferences
                                helper.saveUser(librarian.getId(), librarian.getPhotoUrl(), librarian.getFullName(), librarian.getUsername());

                                //Move next screen
                                startActivity(new Intent(LoginActivity.this, LibrarianActivity.class));
                                finish();
                            }
                        });
            }
        });
    }

    private boolean validateUname(String uname){
        if(uname.isEmpty()){
            uname_layout.setErrorEnabled(true);
            uname_layout.setError("Please enter username");
            return false;
        }
        uname_layout.setErrorEnabled(false);
        uname_layout.setError(null);
        return true;
    }

    private boolean validatePword(String pword){
        if(pword.isEmpty()){
            pword_layout.setErrorEnabled(true);
            pword_layout.setError("Please enter password");
            return false;
        }
        pword_layout.setErrorEnabled(false);
        pword_layout.setError(null);
        return true;
    }
}