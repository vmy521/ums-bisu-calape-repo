package com.bisu.ums_bisucalapelibrary.adapter;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bisu.ums_bisucalapelibrary.EditUserActivity;
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
import com.google.firebase.firestore.WriteBatch;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends FirestoreRecyclerAdapter<User, UserAdapter.ViewHolder> {

    private Context context;
    private FirestoreRecyclerOptions<User> options;
    private Listener listener;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private Helper helper;
    private CircleImageView civ_photo;
    private TextView tv_school_id, tv_name, tv_address, tv_gender, tv_bdate, tv_email, tv_course, tv_date_added, tv_edit, tv_delete;
    private int counter = 0;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public UserAdapter(Context context, @NonNull FirestoreRecyclerOptions<User> options, Listener listener) {
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

        holder.tv_popup_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, holder.tv_popup_menu);
                popupMenu.setForceShowIcon(true);
                popupMenu.inflate(R.menu.menu_user);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.edit:
                                Intent intent = new Intent(context, EditUserActivity.class);
                                intent.putExtra("user", model);
                                context.startActivity(intent);
                                break;
                            case R.id.delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setTitle("Confirm Delete")
                                        .setMessage("Please confirm to delete")
                                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                progressDialog.setTitle("Deleting User");
                                                progressDialog.setMessage("Please wait...");
                                                progressDialog.setCancelable(false);
                                                progressDialog.create();
                                                progressDialog.show();

                                                Query query = db.collection("Monitoring").whereEqualTo("userId", model.getId());
                                                query.get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if(!task.isSuccessful()){
                                                                    progressDialog.hide();
                                                                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    return;
                                                                }

                                                                WriteBatch writeBatch = db.batch();

                                                                writeBatch.delete(db.collection("User").document(model.getId()));

                                                                for(DocumentSnapshot document : task.getResult()){
                                                                    if(counter < 500){
                                                                        Monitoring monitoring = document.toObject(Monitoring.class);
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
                                                                                        counter=0;
                                                                                        Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                                                        notifyDataSetChanged();
                                                                                    }else{
                                                                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                } else {
                                                                    progressDialog.dismiss();
                                                                    writeBatch.commit();
                                                                    counter=0;
                                                                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                                    notifyDataSetChanged();
                                                                }
                                                            }
                                                        });
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .create()
                                        .show();
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.setGravity(Gravity.RIGHT);
                popupMenu.show();
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails(model);
            }
        });
    }

    private void showDetails(User model) {
        final Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_view_user, null);
        dialog.setContentView(view);

        civ_photo = dialog.findViewById(R.id.civ_photo);
        tv_school_id = dialog.findViewById(R.id.tv_school_id);
        tv_name = dialog.findViewById(R.id.tv_name);
        tv_address = dialog.findViewById(R.id.tv_address);
        tv_gender = dialog.findViewById(R.id.tv_gender);
        tv_email = dialog.findViewById(R.id.tv_email);
        tv_bdate = dialog.findViewById(R.id.tv_bdate);
        tv_course = dialog.findViewById(R.id.tv_course);
        tv_date_added = dialog.findViewById(R.id.tv_date_added);
        tv_edit = dialog.findViewById(R.id.tv_edit);
        tv_delete = dialog.findViewById(R.id.tv_delete);

        Glide.with(context)
                .load(model.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.person)
                .into(civ_photo);
        tv_school_id.setText(model.getSchoolId());
        tv_name.setText(model.getFullName());
        tv_address.setText(model.getAddress());
        tv_gender.setText(model.getGender());
        tv_email.setText(model.getEmail());
        tv_bdate.setText(helper.formatDate(model.getBdate()));
        tv_course.setText(model.getCourse());
        tv_date_added.setText(helper.formatDate(model.getDateAdded()));

        tv_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditUserActivity.class);
                intent.putExtra("user", model);
                context.startActivity(intent);
            }
        });

        tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm Delete")
                        .setMessage("Please confirm to delete")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface mDialog, int which) {
                                progressDialog.setTitle("Deleting User");
                                progressDialog.setMessage("Please wait...");
                                progressDialog.setCancelable(false);
                                progressDialog.create();
                                progressDialog.show();

                                Query query = db.collection("Monitoring").whereEqualTo("userId", model.getId());
                                query.get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if(!task.isSuccessful()){
                                                    progressDialog.hide();
                                                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                WriteBatch writeBatch = db.batch();

                                                writeBatch.delete(db.collection("User").document(model.getId()));

                                                for(DocumentSnapshot document : task.getResult()){
                                                    if(counter < 500){
                                                        Monitoring monitoring = document.toObject(Monitoring.class);
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
                                                                        counter = 0;
                                                                        Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                                        notifyDataSetChanged();
                                                                        dialog.dismiss();
                                                                    }else{
                                                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    progressDialog.dismiss();
                                                    writeBatch.commit();
                                                    counter = 0;
                                                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
                                                    notifyDataSetChanged();
                                                    dialog.dismiss();
                                                }
                                            }
                                        });
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
