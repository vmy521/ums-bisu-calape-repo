package com.bisu.ums_bisucalapelibrary;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddUserActivity extends AppCompatActivity {

    private List<String> embeedings;
    private Uri imageUri;
    private CircleImageView civ_photo;
    private TextInputLayout school_id_layout, fullname_layout, address_layout, bdate_layout, age_layout, email_layout, course_layout;
    private AutoCompleteTextView actv_course;
    private RadioGroup gender_group;
    private Button submit_btn;
    private FirebaseFirestore db;
    private StorageReference sref;
    private Helper helper;
    private DatePickerDialog bdatePickerDialog;
    private String selectedBdate;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        embeedings = (List<String>) getIntent().getSerializableExtra("embeedings");
        imageUri = Uri.parse(getIntent().getExtras().getString("imageUri"));

        //Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //For UP button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentforBackButton = NavUtils.getParentActivityIntent(AddUserActivity.this);
                intentforBackButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(AddUserActivity.this, intentforBackButton);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        sref = FirebaseStorage.getInstance().getReference();
        helper = new Helper(this);

        civ_photo = findViewById(R.id.civ_photo);
        school_id_layout = findViewById(R.id.school_id_layout);
        fullname_layout = findViewById(R.id.fullname_layout);
        address_layout = findViewById(R.id.address_layout);
        gender_group = findViewById(R.id.gender_group);
        bdate_layout = findViewById(R.id.bdate_layout);
        age_layout = findViewById(R.id.age_layout);
        email_layout = findViewById(R.id.email_layout);
        course_layout = findViewById(R.id.course_layout);
        actv_course = findViewById(R.id.actv_course);
        submit_btn = findViewById(R.id.submit_btn);
        progressDialog = new ProgressDialog(this);

        //initialize
        init();

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String schoolId = school_id_layout.getEditText().getText().toString().trim();
                String fullName = fullname_layout.getEditText().getText().toString().trim();
                String address = address_layout.getEditText().getText().toString().trim();
                String gender = "";
                if(gender_group.getCheckedRadioButtonId() == R.id.male_btn){
                    gender = "Male";
                }else if(gender_group.getCheckedRadioButtonId() == R.id.female_btn){
                    gender = "Female";
                }
                String email = email_layout.getEditText().getText().toString().trim().toLowerCase();
                String course = actv_course.getText().toString().trim();

                if(!validateSchoolId(schoolId) | !validateFullName(fullName) | !validateAddress(address) | !validateBdate(selectedBdate)
                        | !validateEmail(email) | !validateCourse(course)){
                    return;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("schoolId", schoolId);
                map.put("fullName", helper.capitalize(fullName));
                map.put("address", address);
                map.put("gender", gender);
                map.put("bdate", selectedBdate);
                map.put("email", email);
                map.put("course", course);
                map.put("embeedings", embeedings);
                map.put("dateAdded", FieldValue.serverTimestamp());
                add(map);
            }
        });
    }

    private void add(Map<String, Object> map) {
        progressDialog.setTitle("Adding User");
        progressDialog.setMessage("Please wait");
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        //Check if schoolId already exist
        Query schoolIdQuery = db.collection("User").whereEqualTo("schoolId", map.get("schoolId").toString());
        schoolIdQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            progressDialog.hide();
                            Toast.makeText(AddUserActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            Log.e("App Error", task.getException().getMessage());
                            return;
                        }

                        if(!task.getResult().isEmpty()){
                            progressDialog.hide();
                            school_id_layout.setErrorEnabled(true);
                            school_id_layout.setError("School ID already exist");
                            return;
                        }

                        school_id_layout.setErrorEnabled(false);
                        school_id_layout.setError(null);

                        //Check if email already exist
                        Query emailQuery = db.collection("User").whereEqualTo("email", map.get("email").toString());
                        emailQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(!task.isSuccessful()){
                                    progressDialog.hide();
                                    Toast.makeText(AddUserActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    Log.e("App Error", task.getException().getMessage());
                                    return;
                                }

                                if(!task.getResult().isEmpty()){
                                    progressDialog.hide();
                                    email_layout.setErrorEnabled(true);
                                    email_layout.setError("Email address already exist");
                                    return;
                                }

                                email_layout.setErrorEnabled(false);
                                email_layout.setError(null);

                                String fileName = map.get("fullName").toString();
                                String fileExt = helper.getFileExtFromUri(imageUri);
                                String filePath = "UserImages/" + fileName + "." + fileExt;

                                sref.child(filePath)
                                        .putFile(imageUri)
                                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                            @Override
                                                            public void onSuccess(Uri uri) {
                                                                Toast.makeText(AddUserActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                                                String photoUrl = uri.toString();
                                                                map.put("photoUrl", photoUrl);
                                                                insert(map);
                                                            }
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.e("App Error", e.getMessage());
                                                insert(map);
                                            }
                                        });
                            }
                        });
                    }
                });
    }

    private void insert(Map<String, Object> map) {
        //Remove null|empty values
        map.values().removeAll(Collections.singleton(null));
        map.values().removeAll(Collections.singleton(""));

        db.collection("User").add(map)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(Task<DocumentReference> task) {
                        progressDialog.dismiss();

                        if(task.isSuccessful()){
                            Toast.makeText(AddUserActivity.this, "Success", Toast.LENGTH_SHORT).show();
                            resetFields();

                            //Go back to EnrollFaceActivity
                            Intent intentforBackButton = NavUtils.getParentActivityIntent(AddUserActivity.this);
                            intentforBackButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            NavUtils.navigateUpTo(AddUserActivity.this, intentforBackButton);
                        }else{
                            Toast.makeText(AddUserActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            Log.e("App Error", task.getException().getMessage());
                        }
                    }
                });
    }

    private void init(){
        Glide.with(getApplicationContext())
                .load(imageUri)
                .centerCrop()
                .placeholder(R.drawable.person)
                .into(civ_photo);

        initBdatePicker();
        bdate_layout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bdatePickerDialog.show();
            }
        });

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.course));
        actv_course.setAdapter(courseAdapter);
    }

    private void initBdatePicker(){
        LocalDate ld = LocalDate.now();

        int style = AlertDialog.THEME_HOLO_LIGHT;
        bdatePickerDialog = new DatePickerDialog(AddUserActivity.this, style, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month += 1;

                selectedBdate = year + "-" + (month < 10 ? "0" + month : month) + "-" + (day < 10 ? "0" + day : day);

                bdate_layout.getEditText().setText(helper.formatDate(selectedBdate));
                age_layout.getEditText().setText(helper.calculateAge(selectedBdate));
            }
        }, ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
        bdatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private void resetFields() {
        initBdatePicker();
        selectedBdate = null;
        civ_photo.setImageResource(R.drawable.person);
        school_id_layout.getEditText().setText("");
        fullname_layout.getEditText().setText("");
        address_layout.getEditText().setText("");
        gender_group.clearCheck();
        RadioButton male_btn = findViewById(R.id.male_btn);
        male_btn.setChecked(true);
        bdate_layout.getEditText().setText("");
        age_layout.getEditText().setText("");
        email_layout.getEditText().setText("");
        actv_course.setText("", false);
    }

    private boolean validateSchoolId(String schoolId){
        if(schoolId.isEmpty()){
            school_id_layout.setErrorEnabled(true);
            school_id_layout.setError("Please enter school ID");
            return false;
        }
        school_id_layout.setErrorEnabled(false);
        school_id_layout.setError(null);
        return true;
    }

    private boolean validateFullName(String fullName){
        if(fullName.isEmpty()){
            fullname_layout.setErrorEnabled(true);
            fullname_layout.setError("Please enter full name");
            return false;
        }
        fullname_layout.setErrorEnabled(false);
        fullname_layout.setError(null);
        return true;
    }

    private boolean validateAddress(String address){
        if(address.isEmpty()){
            address_layout.setErrorEnabled(true);
            address_layout.setError("Please enter address");
            return false;
        }
        address_layout.setErrorEnabled(false);
        address_layout.setError(null);
        return true;
    }

    private boolean validateBdate(String bdate){
        if(bdate == null){
            bdate_layout.setErrorEnabled(true);
            bdate_layout.setError("Please select birthday");
            bdate_layout.setErrorIconOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bdatePickerDialog.show();
                }
            });
            return false;
        }
        bdate_layout.setErrorEnabled(false);
        bdate_layout.setError(null);
        return true;
    }

    private boolean validateEmail(String email){
        if(email.isEmpty()){
            email_layout.setErrorEnabled(true);
            email_layout.setError("Please enter email address");
            return false;
        }
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if(!email.matches(emailPattern)){
            email_layout.setErrorEnabled(true);
            email_layout.setError("Invalid email address");
            return false;
        }
        email_layout.setErrorEnabled(false);
        email_layout.setError(null);
        return true;
    }

    private boolean validateCourse(String course){
        if(course.isEmpty()){
            course_layout.setErrorEnabled(true);
            course_layout.setError("Please select course");
            return false;
        }
        course_layout.setErrorEnabled(false);
        course_layout.setError(null);
        return true;
    }

}