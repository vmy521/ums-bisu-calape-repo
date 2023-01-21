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

import com.bisu.ums_bisucalapelibrary.model.Librarian;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ForgotPwordActivity extends AppCompatActivity {

    private TextInputLayout email_layout;
    private Button send_btn;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pword);

        //Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        email_layout = findViewById(R.id.email_layout);
        send_btn = findViewById(R.id.send_btn);
        progressDialog = new ProgressDialog(this);

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = email_layout.getEditText().getText().toString().trim();

                if(!validateEmail(email)){
                    return;
                }

                progressDialog.setTitle("Sending Link");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                //Check if email exist
                Query query = db.collection("Librarian").whereEqualTo("email", email);
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            progressDialog.hide();
                            Toast.makeText(ForgotPwordActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

                        DocumentSnapshot document = task.getResult().iterator().next();
                        Librarian librarian = document.toObject(Librarian.class);

                        //Send password reset link
                        auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressDialog.dismiss();

                                        if(task.isSuccessful()){
                                            Toast.makeText(ForgotPwordActivity.this, "Sent!", Toast.LENGTH_SHORT).show();

                                            //Move back to LoginActivity
                                            startActivity(new Intent(ForgotPwordActivity.this, LoginActivity.class));
                                            finish();
                                        }else{
                                            Toast.makeText(ForgotPwordActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
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
}