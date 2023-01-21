package com.bisu.ums_bisucalapelibrary;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.bisu.ums_bisucalapelibrary.model.Librarian;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyAcctActivity extends AppCompatActivity {

    private CircleImageView civ_photo;
    private TextView tv_name, tv_address, tv_gender, tv_bdate, tv_email, tv_uname;
    private FirebaseFirestore db;
    private Helper helper;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;
    private TextInputLayout cur_email_layout, new_email_layout, new_pword_layout;
    private Button submit_btn;
    private Librarian librarian;
    private int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageReference sref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_acct);

        //Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //For UP button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentforBackButton = NavUtils.getParentActivityIntent(MyAcctActivity.this);
                intentforBackButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(MyAcctActivity.this, intentforBackButton);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        helper = new Helper(this);
        auth = FirebaseAuth.getInstance();
        sref = FirebaseStorage.getInstance().getReference();

        civ_photo = findViewById(R.id.civ_photo);
        tv_name = findViewById(R.id.tv_name);
        tv_address = findViewById(R.id.tv_address);
        tv_gender = findViewById(R.id.tv_gender);
        tv_bdate = findViewById(R.id.tv_bdate);
        tv_email = findViewById(R.id.tv_email);
        tv_uname = findViewById(R.id.tv_uname);
        progressDialog = new ProgressDialog(this);

        setAccountData();
        civ_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            progressDialog.setTitle("Uploading Photo");
            progressDialog.setMessage("Please wait");
            progressDialog.setCancelable(false);
            progressDialog.create();
            progressDialog.show();

            Glide.with(getApplicationContext())
                    .load(imageUri)
                    .centerCrop()
                    .placeholder(R.drawable.person)
                    .into(civ_photo);

            String fileName = librarian.getUsername();
            String fileExt = helper.getFileExtFromUri(imageUri);
            String filePath = "LibrarianImages/" + fileName + "." + fileExt;

            sref.child(filePath)
                    .putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String photoUrl = uri.toString();

                                            Map<String, Object> map = new HashMap<>();
                                            map.put("photoUrl", photoUrl);
                                            db.collection("Librarian").document(librarian.getId()).update(map)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            progressDialog.dismiss();

                                                            if(task.isSuccessful()) {
                                                                Toast.makeText(MyAcctActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                                            }else {
                                                                Toast.makeText(MyAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            progressDialog.dismiss();
                            Log.e("App Error", e.getMessage());
                        }
                    });
        }
    }

    private void setAccountData() {
        db.collection("Librarian").document(helper.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(MyAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        librarian = task.getResult().toObject(Librarian.class);

                        Glide.with(getApplicationContext())
                                .load(librarian.getPhotoUrl())
                                .centerCrop()
                                .placeholder(R.drawable.person)
                                .into(civ_photo);
                        tv_name.setText(librarian.getFullName());
                        tv_address.setText(librarian.getAddress());
                        tv_gender.setText(librarian.getGender());
                        tv_bdate.setText(helper.formatDate(librarian.getBdate()));
                        tv_email.setText(librarian.getEmail());
                        tv_uname.setText(librarian.getUsername());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_acct, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.change_pass:
                changePass();
                break;
            case R.id.change_email:
                changeEmail();
                break;
            case R.id.logout:
                progressDialog.setTitle("Logging out");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                logout();
                break;
        }
        return true;
    }

    private void changeEmail() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        dialog.setContentView(view);

        cur_email_layout = dialog.findViewById(R.id.cur_email_layout);
        new_email_layout = dialog.findViewById(R.id.new_email_layout);
        submit_btn = dialog.findViewById(R.id.submit_btn);

        cur_email_layout.getEditText().setText(librarian.getEmail());

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String curEmail = cur_email_layout.getEditText().getText().toString().trim();
                String newEmail = new_email_layout.getEditText().getText().toString().trim();

                if(!validateNewEmail(curEmail, newEmail)){
                    return;
                }

                progressDialog.setTitle("Updating email");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                auth.getCurrentUser().updateEmail(newEmail)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()){
                                    progressDialog.hide();
                                    Toast.makeText(MyAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Map<String, Object> map = new HashMap<>();
                                map.put("email", newEmail);

                                db.collection("Librarian").document(librarian.getId()).update(map)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                progressDialog.dismiss();

                                                if(task.isSuccessful()){
                                                    Toast.makeText(MyAcctActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }else{
                                                    Toast.makeText(MyAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        });
            }
        });

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void changePass() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_pword, null);
        dialog.setContentView(view);

        new_pword_layout = dialog.findViewById(R.id.new_pword_layout);
        submit_btn = dialog.findViewById(R.id.submit_btn);

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPword = new_pword_layout.getEditText().getText().toString().trim();

                if(!validatePword(newPword)){
                    return;
                }

                progressDialog.setTitle("Updating password");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                auth.getCurrentUser().updatePassword(newPword)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressDialog.dismiss();

                                if(task.isSuccessful()) {
                                    Toast.makeText(MyAcctActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }else {
                                    Toast.makeText(MyAcctActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void logout() {
        auth.signOut();
        helper.clearUser();
        progressDialog.dismiss();
        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean validateNewEmail(String curEmail, String newEmail){
        if(newEmail.isEmpty()){
            new_email_layout.setErrorEnabled(true);
            new_email_layout.setError("Please enter new email address");
            return false;
        }
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if(!newEmail.matches(emailPattern)){
            new_email_layout.setErrorEnabled(true);
            new_email_layout.setError("Invalid new email address");
            return false;
        }

        if(newEmail == curEmail){
            new_email_layout.setErrorEnabled(true);
            new_email_layout.setError("You entered current email address");
            return false;
        }

        new_email_layout.setErrorEnabled(false);
        new_email_layout.setError(null);
        return true;
    }

    private boolean validatePword(String pword){
        if(pword.isEmpty()){
            new_pword_layout.setErrorEnabled(true);
            new_pword_layout.setError("Please enter new password");
            return false;
        }else{
            if(pword.length() < 6){
                new_pword_layout.setErrorEnabled(true);
                new_pword_layout.setError("Password must be at least 6 characters");
                return false;
            }
            new_pword_layout.setErrorEnabled(false);
            new_pword_layout.setError(null);
            return true;
        }
    }

}