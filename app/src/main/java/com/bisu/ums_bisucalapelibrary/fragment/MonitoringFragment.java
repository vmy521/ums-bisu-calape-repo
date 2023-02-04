package com.bisu.ums_bisucalapelibrary.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.Listener;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.adapter.ExportAdapter;
import com.bisu.ums_bisucalapelibrary.adapter.MonitoringAdapter;
import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.bisu.ums_bisucalapelibrary.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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
 * Use the {@link MonitoringFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MonitoringFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView rv;
    private TextView tv_noresult, tv_total;
    private MonitoringAdapter adapter;
    private FirebaseFirestore db;
    private String search = "";
    private Helper helper;

    //Export Dialog
    private String dialog_search = "";
    private TextInputLayout search_layout;
    private RecyclerView dialog_rv;
    private TextView dialog_tv_noresult;
    private Button export_btn;
    private ExportAdapter exportAdapter;
    private static int STORAGE_PERMISSION_CODE = 100;
    private ProgressDialog progressDialog;

    public MonitoringFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MonitoringFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MonitoringFragment newInstance(String param1, String param2) {
        MonitoringFragment fragment = new MonitoringFragment();
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
        return inflater.inflate(R.layout.fragment_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        helper = new Helper(getContext());

        tv_total = view.findViewById(R.id.tv_total);
        rv = view.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        tv_noresult = view.findViewById(R.id.tv_noresult);
        progressDialog = new ProgressDialog(getContext());

        setOptionsMenu();
    }

    private void load(){
        Query query;

        if(!search.isEmpty()){
            query = db.collection("Monitoring")
                    .whereEqualTo("fullName", search)
                    .orderBy("timeIn", Query.Direction.DESCENDING);
        }else{
            query = db.collection("Monitoring")
                    .orderBy("timeIn", Query.Direction.DESCENDING);
        }

        FirestoreRecyclerOptions<Monitoring> options = new FirestoreRecyclerOptions.Builder<Monitoring>()
                .setQuery(query, Monitoring.class)
                .build();

        Listener listener = new Listener() {
            @Override
            public void setTotal(int total) {
                if(total > 0){
                    tv_noresult.setVisibility(View.INVISIBLE);
                }else{
                    tv_noresult.setVisibility(View.VISIBLE);
                }
                System.out.println(String.valueOf(total));
                tv_total.setText(String.valueOf(total));
            }
        };

        adapter = new MonitoringAdapter(getContext(), options, listener, tv_noresult);
        rv.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        load();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void setOptionsMenu(){
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if(!menu.hasVisibleItems()){
                    menuInflater.inflate(R.menu.menu_monitoring_menu, menu);
                }

                MenuItem menuItem = menu.findItem(R.id.search);
                SearchView searchView = (SearchView) menuItem.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        search = query.trim();
                        if (!search.isEmpty()) {
                            search = helper.capitalize(search);
                        }
                        load();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        search = newText.trim();
                        if (search.isEmpty()) {
                            load();
                        }
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.export:
                        showExportDialog();
                        break;
                }
                return true;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showExportDialog() {
        Dialog dialog = new Dialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_export, null);
        dialog.setContentView(view);

        search_layout = dialog.findViewById(R.id.search_layout);
        dialog_rv = dialog.findViewById(R.id.rv);
        dialog_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        dialog_tv_noresult = dialog.findViewById(R.id.tv_noresult);
        export_btn = dialog.findViewById(R.id.export_btn);

        loadUsers();

        search_layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                dialog_search = s.toString().trim();
                if (!dialog_search.isEmpty()) {
                    dialog_search = helper.capitalize(dialog_search);
                }
                loadUsers();
            }
        });

        export_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAllMonitoringData();
            }
        });

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog_search = "";
            }
        });
    }

    private void getAllMonitoringData() {
        progressDialog.setTitle("Exporting");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        FirebaseFirestore.getInstance().collection("Monitoring")
                .orderBy("fullName", Query.Direction.ASCENDING)
                .orderBy("timeIn", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()) {
                            progressDialog.hide();
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(task.getResult().getDocuments().size() < 1) {
                            progressDialog.hide();
                            Toast.makeText(getContext(), "Nothing to export", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        generateReport(task.getResult().getDocuments());
                    }
                });
    }

    private void generateReport(List<DocumentSnapshot> documents) {
        Workbook wb = new HSSFWorkbook();
        Cell cell = null;
        Sheet sheet = null;

        String sheetName = helper.getNumberDate(LocalDate.now().toString());
        sheet = wb.createSheet("Report (" + sheetName + ")");

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

        int size = documents.size();
        List<DocumentSnapshot> documentSnapshots = documents;
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
        String folderName = "UMS/Monitoring/All";
        String fileName = sheetName + ".xls";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + folderName + "/" + fileName;

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + folderName);
        if (!file.exists()) {
            file.mkdirs();
        }

        FileOutputStream outputStream = null;

        progressDialog.dismiss();
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


    private void loadUsers(){
        Query query;

        if(!dialog_search.isEmpty()){
            query = db.collection("User")
                    .orderBy("fullName")
                    .startAt(dialog_search)
                    .endAt(dialog_search + "\uf8ff");
        }else{
            query = db.collection("User").orderBy("fullName");
        }

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        Listener listener = new Listener() {
            @Override
            public void setTotal(int total) {
                if(total > 0){
                    dialog_tv_noresult.setVisibility(View.INVISIBLE);
                }else{
                    dialog_tv_noresult.setVisibility(View.VISIBLE);
                }
                //System.out.println(String.valueOf(total));
                //tv_total.setText(String.valueOf(total));
            }
        };

        exportAdapter = new ExportAdapter(getContext(), options, listener);
        dialog_rv.setAdapter(exportAdapter);
        exportAdapter.startListening();
    }

}