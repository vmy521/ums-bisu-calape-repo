package com.bisu.ums_bisucalapelibrary.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextInputLayout from_layout, to_layout;
    private Button generate_btn;
    private FirebaseFirestore db;
    private Helper helper;
    private String selectedFrom, selectedTo;
    private DatePickerDialog fromDatePickerDialog, toDatePickerDialog;
    private static int STORAGE_PERMISSION_CODE = 100;

    public ReportFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReportFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportFragment newInstance(String param1, String param2) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        helper = new Helper(getContext());

        from_layout = view.findViewById(R.id.from_layout);
        to_layout = view.findViewById(R.id.to_layout);
        generate_btn = view.findViewById(R.id.generate_btn);

        from_layout.getEditText().setText(helper.formatDate(LocalDate.now().toString()));

        initFromDatePicker();
        from_layout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fromDatePickerDialog.show();
            }
        });
        selectedFrom = helper.getNumberDate(LocalDate.now().toString());

        initToDatePicker();
        to_layout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDatePickerDialog.show();
            }
        });

        generate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()) {
                    generateReport();
                }else {
                    requestPermission();
                }
            }
        });
    }

    private void generateReport() {
        //Get report data from Monitoring collection
        Query query = db.collection("Monitoring")
                        .orderBy("timeIn", Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.parse(selectedFrom)));

        if(selectedTo != null){
            query = query.whereLessThanOrEqualTo("timeIn", helper.getEndDate(LocalDate.parse(selectedTo)));
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(task.getResult().isEmpty()){
                    Toast.makeText(getContext(), "No result!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Workbook wb = new HSSFWorkbook();
                Cell cell = null;
                Sheet sheet = null;

                String sheetDate = helper.formatDate(selectedFrom);
                if(selectedTo != null){
                    sheetDate += " - " + helper.formatDate(selectedTo);
                }
                sheet = wb.createSheet("Report (" + sheetDate + ")");

                Row row = sheet.createRow(0);

                cell = row.createCell(0);
                cell.setCellValue("No.");
                cell = row.createCell(1);
                cell.setCellValue("Name");
                cell = row.createCell(2);
                cell.setCellValue("Gender");
                cell = row.createCell(3);
                cell.setCellValue("Age Group");
                cell = row.createCell(4);
                cell.setCellValue("Course");
                cell = row.createCell(5);
                cell.setCellValue("Purpose of Visit");
                cell = row.createCell(6);
                cell.setCellValue("Time In");
                cell = row.createCell(7);
                cell.setCellValue("Time Out");

                //column width
                sheet.setColumnWidth(0, (20 * 100));
                sheet.setColumnWidth(1, (30 * 200));
                sheet.setColumnWidth(2, (30 * 200));
                sheet.setColumnWidth(3, (30 * 200));
                sheet.setColumnWidth(4, (30 * 200));
                sheet.setColumnWidth(5, (30 * 400));
                sheet.setColumnWidth(6, (30 * 200));
                sheet.setColumnWidth(7, (30 * 200));

                int size = task.getResult().size();
                List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                int counter = 1;

                for(int i=0; i<size; i++){
                    Monitoring monitoring = documentSnapshots.get(i).toObject(Monitoring.class);

                    Row tempRow = sheet.createRow(i + 1);

                    cell = tempRow.createCell(0);
                    cell.setCellValue(String.valueOf(counter++));

                    cell = tempRow.createCell(1);
                    cell.setCellValue(monitoring.getFullName());

                    cell = tempRow.createCell(2);
                    cell.setCellValue(monitoring.getGender());

                    cell = tempRow.createCell(3);
                    int age = Integer.parseInt(helper.calculateAge(monitoring.getBdate()));
                    if(age >= 18 && age <= 20){
                        cell.setCellValue("18-20");
                    }else if(age >= 21 && age <= 23){
                        cell.setCellValue("21-23");
                    }else if(age >= 24 && age <= 26){
                        cell.setCellValue("24-26");
                    }else {
                        cell.setCellValue("27 and Above");
                    }


                    cell = tempRow.createCell(4);
                    cell.setCellValue(monitoring.getCourse());

                    cell = tempRow.createCell(5);
                    cell.setCellValue(monitoring.getPurposeVisit());

                    cell = tempRow.createCell(6);
                    cell.setCellValue(helper.formatDate(monitoring.getTimeIn()));

                    cell = tempRow.createCell(7);
                    if(monitoring.getTimeOut() != null){
                        cell.setCellValue(helper.formatDate(monitoring.getTimeOut()));
                    }else{
                        cell.setCellValue("No Timeout");
                    }

                    sheet.setColumnWidth(0, (20 * 100));
                    sheet.setColumnWidth(1, (30 * 200));
                    sheet.setColumnWidth(2, (30 * 200));
                    sheet.setColumnWidth(3, (30 * 200));
                    sheet.setColumnWidth(4, (30 * 200));
                    sheet.setColumnWidth(5, (30 * 400));
                    sheet.setColumnWidth(6, (30 * 200));
                    sheet.setColumnWidth(7, (30 * 200));
                }

                //Export xls
                String folderName = "UMS/Report";
                String fileName = helper.getNumberDate(LocalDate.now().toString()) + ".xls";
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + folderName + "/" + fileName;

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + folderName);
                if (!file.exists()) {
                    file.mkdirs();
                }

                FileOutputStream outputStream = null;

                try {
                    outputStream = new FileOutputStream(path);
                    wb.write(outputStream);
                    Toast.makeText(getContext(), "File saved in " + path, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed", Toast.LENGTH_LONG).show();

                    try {
                        outputStream.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();

                    }
                }
            }
        });
    }

    private void requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }catch (Exception e) {
                e.printStackTrace();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    public boolean checkPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if(Environment.isExternalStorageManager()) {
                            generateReport();
                        }else {
                            //Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.length > 0) {
                boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if(write && read) {
                    generateReport();
                }else {
                    Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initFromDatePicker(){
        LocalDate ld = LocalDate.now();

        int style = AlertDialog.THEME_HOLO_LIGHT;
        fromDatePickerDialog = new DatePickerDialog(getContext(), style, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month += 1;

                selectedFrom = year + "-" + (month < 10 ? "0" + month : month) + "-" + (day < 10 ? "0" + day : day);

                from_layout.getEditText().setText(helper.formatDate(selectedFrom));
            }
        }, ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
        fromDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private void initToDatePicker(){
        LocalDate ld = LocalDate.now();

        int style = AlertDialog.THEME_HOLO_LIGHT;
        toDatePickerDialog = new DatePickerDialog(getContext(), style, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month += 1;

                selectedTo = year + "-" + (month < 10 ? "0" + month : month) + "-" + (day < 10 ? "0" + day : day);

                to_layout.getEditText().setText(helper.formatDate(selectedTo));
            }
        }, ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
        toDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

}