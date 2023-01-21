package com.bisu.ums_bisucalapelibrary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bisu.ums_bisucalapelibrary.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class CreateAcctActivity extends AppCompatActivity {

    private TextInputLayout email_layout, uname_layout, pword_layout;
    private Button submit_btn;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acct);

        //Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        email_layout = findViewById(R.id.email_layout);
        uname_layout = findViewById(R.id.uname_layout);
        pword_layout = findViewById(R.id.pword_layout);
        submit_btn = findViewById(R.id.submit_btn);
        progressDialog = new ProgressDialog(this);

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = email_layout.getEditText().getText().toString().trim();
                String uname = uname_layout.getEditText().getText().toString().trim();
                String pword = pword_layout.getEditText().getText().toString().trim();

                if(!validateEmail(email) | !validateUname(uname) | !validatePword(pword)){
                    return;
                }

                progressDialog.setTitle("Creating Account");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                //Check if email exist
                Query query = db.collection("User").whereEqualTo("email", email);
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            progressDialog.hide();
                            Toast.makeText(CreateAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(task.getResult().isEmpty()){
                            progressDialog.hide();
                            email_layout.setErrorEnabled(true);
                            email_layout.setError("Email address not found");
                            return;
                        }

                        email_layout.setErrorEnabled(false);
                        email_layout.setError(null);

                        final User user = task.getResult().iterator().next().toObject(User.class);

                        //Check if username exist
                        Query query = db.collection("User").whereEqualTo("username", uname);
                        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(!task.isSuccessful()){
                                    progressDialog.hide();
                                    Toast.makeText(CreateAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if(!task.getResult().isEmpty()){
                                    progressDialog.hide();
                                    uname_layout.setErrorEnabled(true);
                                    uname_layout.setError("Username already exist");
                                    return;
                                }

                                uname_layout.setErrorEnabled(false);
                                uname_layout.setError(null);

                                //Create user in firebaseAuth
                                auth.createUserWithEmailAndPassword(email, pword)
                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if(!task.isSuccessful()){
                                                    progressDialog.hide();
                                                    Toast.makeText(CreateAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                db.collection("User").document(user.getId()).update("username", uname)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                progressDialog.dismiss();

                                                                if(task.isSuccessful()){
                                                                    Toast.makeText(CreateAcctActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                                                                    auth.signOut();

                                                                    //Move back to LoginActivity
                                                                    startActivity(new Intent(CreateAcctActivity.this, LoginActivity.class));
                                                                    finish();
                                                                }else{
                                                                    Toast.makeText(CreateAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
                    }
                });
            }
        });
    }

    private boolean validateEmail(String email) {
        if(email.isEmpty()){
            email_layout.setErrorEnabled(true);
            email_layout.setError("Please enter email address");
            return false;
        }
        email_layout.setErrorEnabled(false);
        email_layout.setError(null);
        return true;
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
        }else{
            if(pword.length() < 6){
                pword_layout.setErrorEnabled(true);
                pword_layout.setError("Password must be at least 6 characters");
                return false;
            }
            pword_layout.setErrorEnabled(false);
            pword_layout.setError(null);
            return true;
        }
    }
}