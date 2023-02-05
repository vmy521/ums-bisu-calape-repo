package com.bisu.ums_bisucalapelibrary.adapter;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.Listener;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.model.MainViewModel;
import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MonitoringAdapter extends FirestoreRecyclerAdapter<Monitoring, MonitoringAdapter.ViewHolder> {

    private Context context;
    private FirestoreRecyclerOptions<Monitoring> options;
    private Listener listener;
    private TextView tv_noresult;
    private Helper helper;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private MainViewModel mainViewModel;
    private boolean isEnable = false;
    private boolean isSelectAll = false;
    private List<Monitoring> selectList;
    private int counter = 0;
    private CircleImageView civ_photo;
    private TextView tv_school_id, tv_name, tv_gender, tv_bdate, tv_age_group, tv_course, tv_purpose_visit, tv_time_in_out, tv_delete;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public MonitoringAdapter(Context context, @NonNull FirestoreRecyclerOptions<Monitoring> options, Listener listener, TextView tv_noresult) {
        super(options);
        this.context = context;
        this.options = options;
        this.listener = listener;
        this.tv_noresult = tv_noresult;
        selectList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        helper = new Helper(context);
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Monitoring model) {
        Glide.with(holder.itemView.getContext())
                .load(model.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.person)
                .circleCrop()
                .into(holder.civ_photo);
        holder.tv_fullname.setText(model.getFullName());
        holder.tv_course.setText(model.getCourse());
        if(model.getTimeIn() != null){
            holder.tv_time_in.setText(helper.formatDate(model.getTimeIn()));
        }else{
            holder.tv_time_in.setText("No time-in");
        }

        if(model.getTimeOut() != null){
            holder.tv_time_out.setText(helper.formatDate(model.getTimeOut()));
        }else {
            holder.tv_time_out.setText("No time-out");
        }

        holder.tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm Delete")
                        .setMessage("Please confirm to delete")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.setTitle("Deleting");
                                progressDialog.setMessage("Please wait...");
                                progressDialog.setCancelable(false);
                                progressDialog.create();
                                progressDialog.show();

                                if(helper.isConnected()) {
                                    db.collection("Monitoring").document(model.getId()).delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    progressDialog.dismiss();

                                                    if(task.isSuccessful()){
                                                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
                                                        notifyDataSetChanged();
                                                    }else{
                                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    progressDialog.dismiss();
                                    db.collection("Monitoring").document(model.getId()).delete();
                                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!isEnable){
                    holder.tv_delete.setVisibility(View.GONE);

                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                            MenuInflater inflater = actionMode.getMenuInflater();
                            inflater.inflate(R.menu.menu_monitoring_action_mode, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            isEnable = true;
                            clickItem(holder, model);

                            mainViewModel.getText().observe((FragmentActivity) context, new Observer<String>() {
                                @Override
                                public void onChanged(String s) {
                                    actionMode.setTitle(String.format("%s selected", s));
                                }
                            });
                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case R.id.delete_all:
                                    if(selectList.size() < 1){
                                        Toast.makeText(context, "Select at least 1 item", Toast.LENGTH_SHORT).show();
                                        return false;
                                    }

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Confirm Delete")
                                            .setMessage("Please confirm to delete")
                                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    progressDialog.setTitle("Deleting");
                                                    progressDialog.setMessage("Please wait...");
                                                    progressDialog.setCancelable(false);
                                                    progressDialog.create();
                                                    progressDialog.show();

                                                    WriteBatch writeBatch = db.batch();
                                                    for(Monitoring monitoring : selectList){
                                                        if(counter < 500){
                                                            writeBatch.delete(db.collection("Monitoring").document(monitoring.getId()));
                                                            counter++;
                                                        }
                                                    }

                                                    if(helper.isConnected()) {
                                                        writeBatch.commit()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        progressDialog.dismiss();

                                                                        if(task.isSuccessful()){
                                                                            Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                                            counter = 0;
                                                                            selectList.clear();

                                                                            if(getSnapshots().size() < 1){
                                                                                tv_noresult.setVisibility(View.VISIBLE);
                                                                            }

                                                                            notifyDataSetChanged();
                                                                            actionMode.finish();
                                                                        }else{
                                                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    }else {
                                                        progressDialog.dismiss();
                                                        writeBatch.commit();
                                                        Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                        counter = 0;
                                                        selectList.clear();

                                                        if(getSnapshots().size() < 1){
                                                            tv_noresult.setVisibility(View.VISIBLE);
                                                        }

                                                        notifyDataSetChanged();
                                                        actionMode.finish();
                                                    }
                                                }
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .create()
                                            .show();
                                    break;
                                case R.id.select_all:
                                    if(selectList.size() == getSnapshots().size()){
                                        isSelectAll = false;
                                        selectList.clear();
                                    }else{
                                        isSelectAll = true;
                                        selectList.clear();
                                        selectList.addAll(getSnapshots());
                                    }

                                    mainViewModel.setText(String.valueOf(selectList.size()));
                                    notifyDataSetChanged();
                                    break;
                            }
                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {
                            holder.tv_delete.setVisibility(View.VISIBLE);
                            isEnable = false;
                            isSelectAll = false;
                            selectList.clear();
                            notifyDataSetChanged();
                        }
                    };

                    ((AppCompatActivity) context).startActionMode(callback);
                }else{
                    clickItem(holder, model);
                }
                return true;
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isEnable){
                    clickItem(holder, model);
                }else{
                    showDetails(model);
                }
            }
        });

        if(isSelectAll){
            holder.tv_delete.setVisibility(View.GONE);
            holder.tv_check.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        }else{
            holder.tv_delete.setVisibility(View.VISIBLE);
            holder.tv_check.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    private void showDetails(Monitoring model) {
        final Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_view_monitoring, null);
        dialog.setContentView(view);

        civ_photo = dialog.findViewById(R.id.civ_photo);
        tv_school_id = dialog.findViewById(R.id.tv_school_id);
        tv_name = dialog.findViewById(R.id.tv_name);
        tv_gender = dialog.findViewById(R.id.tv_gender);
        tv_bdate = dialog.findViewById(R.id.tv_bdate);
        tv_age_group = dialog.findViewById(R.id.tv_age_group);
        tv_course = dialog.findViewById(R.id.tv_course);
        tv_purpose_visit = dialog.findViewById(R.id.tv_purpose_visit);
        tv_time_in_out = dialog.findViewById(R.id.tv_time_in_out);
        tv_delete = dialog.findViewById(R.id.tv_delete);

        Glide.with(context)
                .load(model.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.person)
                .into(civ_photo);
        tv_school_id.setText(model.getSchoolId());
        tv_name.setText(model.getFullName());
        tv_gender.setText(model.getGender());
        tv_bdate.setText(helper.formatDate(model.getBdate()));
        int age = Integer.parseInt(helper.calculateAge(model.getBdate()));
        if(age >= 18 && age <= 20){
            tv_age_group.setText("18-20");
        }else if(age >= 21 && age <= 23){
            tv_age_group.setText("21-23");
        }else if(age >= 24 && age <= 26){
            tv_age_group.setText("24-26");
        }else {
            tv_age_group.setText("27 and Above");
        }
        tv_course.setText(model.getCourse());
        tv_purpose_visit.setText(model.getPurposeVisit());
        String dateTime = helper.formatDate(model.getTimeIn());
        if(model.getTimeOut() != null){
            dateTime += " / " + helper.formatDate(model.getTimeOut());
        }
        tv_time_in_out.setText(dateTime);

        tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm Delete")
                        .setMessage("Please confirm to delete")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface mDialog, int which) {
                                progressDialog.setTitle("Deleting");
                                progressDialog.setMessage("Please wait...");
                                progressDialog.setCancelable(false);
                                progressDialog.create();
                                progressDialog.show();

                                if(helper.isConnected()) {
                                    db.collection("Monitoring").document(model.getId()).delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    progressDialog.dismiss();

                                                    if(task.isSuccessful()){
                                                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
                                                        notifyDataSetChanged();
                                                        dialog.dismiss();
                                                    }else{
                                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    progressDialog.dismiss();
                                    db.collection("Monitoring").document(model.getId()).delete();
                                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            }
        });

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void clickItem(ViewHolder holder, Monitoring model) {
        if(holder.tv_check.getVisibility() == View.GONE){
            holder.tv_delete.setVisibility(View.GONE);
            holder.tv_check.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.LTGRAY);
            selectList.add(model);
        }else{
            holder.tv_delete.setVisibility(View.VISIBLE);
            holder.tv_check.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
            selectList.remove(model);
        }

        mainViewModel.setText(String.valueOf(selectList.size()));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.monitoring_item, parent, false);
        mainViewModel = ViewModelProviders.of((FragmentActivity) context).get(MainViewModel.class);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        if(listener != null){
            listener.setTotal(getSnapshots().size());
        }
        return getSnapshots().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView civ_photo;
        private TextView tv_fullname, tv_course, tv_time_in, tv_time_out, tv_delete, tv_check;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            civ_photo = itemView.findViewById(R.id.civ_photo);
            tv_fullname = itemView.findViewById(R.id.tv_fullname);
            tv_course = itemView.findViewById(R.id.tv_course);
            tv_time_in = itemView.findViewById(R.id.tv_time_in);
            tv_time_out = itemView.findViewById(R.id.tv_time_out);
            tv_delete = itemView.findViewById(R.id.tv_delete);
            tv_check = itemView.findViewById(R.id.tv_check);
        }
    }

}
