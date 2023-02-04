package com.bisu.ums_bisucalapelibrary.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.Listener;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.bisu.ums_bisucalapelibrary.model.User;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExportAdapter extends FirestoreRecyclerAdapter<User, ExportAdapter.ViewHolder> {

    private Context context;
    private FirestoreRecyclerOptions<User> options;
    private Listener listener;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private Helper helper;
    private static int STORAGE_PERMISSION_CODE = 100;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public ExportAdapter(Context context, @NonNull FirestoreRecyclerOptions<User> options, Listener listener) {
        super(options);
        this.context = context;
        this.options = options;
        this.listener = listener;
        progressDialog = new ProgressDialog(context);
        db = FirebaseFirestore.getInstance();
        helper = new Helper(context);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull User model) {
        Glide.with(holder.itemView.getContext())
                .load(model.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.person)
                .circleCrop()
                .into(holder.civ_photo);
        holder.tv_fullname.setText(model.getFullName());
        holder.tv_school_id.setText(model.getSchoolId());
        holder.tv_course.setText(model.getCourse());
        holder.tv_popup_menu.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserMonitoringData(model);
            }
        });
    }

    private void getUserMonitoringData(User user) {
        progressDialog.setTitle("Exporting");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        FirebaseFirestore.getInstance().collection("Monitoring")
                .whereEqualTo("userId", user.getId())
                .orderBy("timeIn", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()) {
                            progressDialog.hide();
                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(task.getResult().getDocuments().size() < 1) {
                            progressDialog.hide();
                            Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String sheetName = Arrays.stream(user.getFullName().toLowerCase().split(" ")).collect(Collectors.joining("-"));
                        generateReport(task.getResult().getDocuments(), sheetName  + "-" + helper.getNumberDate(LocalDate.now().toString()));
                    }
                });
    }

    private void generateReport(List<DocumentSnapshot> documents, String sheetName) {
        Workbook wb = new HSSFWorkbook();
        Cell cell = null;
        Sheet sheet = null;

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
        String folderName = "UMS/Monitoring/User";
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
            Toast.makeText(context, "File saved in " + path, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();

            try {
                outputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();

            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        final int total = options.getSnapshots() != null ? options.getSnapshots().size() : 0;
        if(listener != null){
            listener.setTotal(total);
        }
        return total;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView civ_photo;
        private TextView tv_fullname, tv_school_id, tv_course, tv_popup_menu, tv_check;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            civ_photo = itemView.findViewById(R.id.civ_photo);
            tv_fullname = itemView.findViewById(R.id.tv_fullname);
            tv_school_id = itemView.findViewById(R.id.tv_school_id);
            tv_course = itemView.findViewById(R.id.tv_course);
            tv_popup_menu = itemView.findViewById(R.id.tv_popup_menu);
            tv_check = itemView.findViewById(R.id.tv_check);
        }
    }

}
