package com.bisu.ums_bisucalapelibrary.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DashboardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView tv_date, tv_users;
    private PieChart gender_pie_chart, age_pie_chart, course_pie_chart, purpose_pie_chart;
    private List<PieEntry> gender_pieEntryList, age_pieEntryList, course_PieEntryList, purpose_PieEntryList;
    private FirebaseFirestore db;
    private Helper helper;
    private int total = 0, total18_20 = 0, total21_23 = 0, total24_26 = 0, total27_Above= 0;

    public DashboardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DashboardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DashboardFragment newInstance(String param1, String param2) {
        DashboardFragment fragment = new DashboardFragment();
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
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        helper = new Helper(getContext());

        tv_users = view.findViewById(R.id.tv_users);
        tv_date = view.findViewById(R.id.tv_date);
        gender_pie_chart = view.findViewById(R.id.gender_pie_chart);
        age_pie_chart = view.findViewById(R.id.age_pie_chart);
        course_pie_chart = view.findViewById(R.id.course_pie_chart);
        purpose_pie_chart = view.findViewById(R.id.purpose_pie_chart);
        gender_pieEntryList = new ArrayList<>();
        age_pieEntryList = new ArrayList<>();
        course_PieEntryList = new ArrayList<>();
        purpose_PieEntryList = new ArrayList<>();

        tv_date.setText(helper.formatDate(LocalDate.now().toString()));
        setTotalUsers();
        setMonitoringByGender();
        setMonitoringByAge();
        setMonitoringByCourse();
        setMonitoringByPurposeVisit();
    }

    private void setTotalUsers() {
        db.collection("User").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        tv_users.setText(String.valueOf(task.getResult().size()));
                    }
                });
    }

    private void setMonitoringByGender() {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        Query totalQuery = null, maleQuery = null, femaleQuery = null;

        totalQuery = db.collection("Monitoring")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        maleQuery = db.collection("Monitoring")
                .whereEqualTo("gender", "Male")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        femaleQuery = db.collection("Monitoring")
                .whereEqualTo("gender", "Female")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));

        //Get total users
        Task<QuerySnapshot> task1 = totalQuery.get();

        //Get total male users
        Task<QuerySnapshot> task2 = maleQuery.get();

        //Get total female users
        Task<QuerySnapshot> task3 = femaleQuery.get();

        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(new OnCompleteListener<List<Object>>() {
            @Override
            public void onComplete(Task<List<Object>> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    Log.e("App Error", task.getException().getMessage());
                    return;
                }

                QuerySnapshot totalSnapshot = (QuerySnapshot) task.getResult().get(0);
                QuerySnapshot maleSnapshot = (QuerySnapshot) task.getResult().get(1);
                QuerySnapshot femaleSnapshot = (QuerySnapshot) task.getResult().get(2);

                gender_pie_chart.setDrawHoleEnabled(true);
                //pie_chart.setUsePercentValues(true);
                gender_pie_chart.setEntryLabelTextSize(16f);
                gender_pie_chart.setEntryLabelColor(Color.BLACK);
                gender_pie_chart.setCenterText("By Gender");
                gender_pie_chart.setCenterTextSize(16f);
                gender_pie_chart.getDescription().setEnabled(false);
                gender_pie_chart.setExtraOffsets(20, 20, 20, 20);
                gender_pie_chart.setDrawEntryLabels(false);

                Legend l = gender_pie_chart.getLegend();
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                l.setDrawInside(false);
                l.setEnabled(true);
                l.setTextSize(12f);
                l.setYOffset(-25);

                gender_pieEntryList.clear();
                gender_pieEntryList.add(new PieEntry(maleSnapshot.size(), "Male"));
                gender_pieEntryList.add(new PieEntry(femaleSnapshot.size(), "Female"));

                PieDataSet pieDataSet = new PieDataSet(gender_pieEntryList, "");
                pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                pieDataSet.setValueTextColor(Color.BLACK);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(true);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if(value == 0.0f){
                            return "";
                        }
                        return String.valueOf((int) value);
                    }
                });
                pieData.setValueTextSize(16f);
                pieData.setValueTextColor(Color.BLACK);

                gender_pie_chart.setData(pieData);
                gender_pie_chart.invalidate();
                gender_pie_chart.animate();
            }
        });
    }

    private void setMonitoringByAge() {
        Query query = db.collection("Monitoring")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                total = task.getResult().size();
                LocalDate minDOB20 = helper.findMinDOBOfAge(20);
                LocalDate minDOB18 = helper.findMinDOBOfAge(18);
                LocalDate minDOB23 = helper.findMinDOBOfAge(23);
                LocalDate minDOB21 = helper.findMinDOBOfAge(21);
                LocalDate minDOB26 = helper.findMinDOBOfAge(26);
                LocalDate minDOB24 = helper.findMinDOBOfAge(24);

                for(DocumentSnapshot document : task.getResult()){
                    Monitoring monitoring = document.toObject(Monitoring.class);

                    LocalDate dob = LocalDate.parse(monitoring.getBdate());

                    if(dob.isAfter(minDOB20) && dob.isBefore(minDOB18)){
                        total18_20++;
                    }else if(dob.isAfter(minDOB23) && dob.isBefore(minDOB21)){
                        total21_23++;
                    }else if(dob.isAfter(minDOB26) && dob.isBefore(minDOB24)){
                        total24_26++;
                    }else {
                        total27_Above++;
                    }
                }

                age_pie_chart.setDrawHoleEnabled(true);
                //pie_chart.setUsePercentValues(true);
                age_pie_chart.setEntryLabelTextSize(16f);
                age_pie_chart.setEntryLabelColor(Color.BLACK);
                age_pie_chart.setCenterText("By Age");
                age_pie_chart.setCenterTextSize(16f);
                age_pie_chart.getDescription().setEnabled(false);
                age_pie_chart.setExtraOffsets(20, 20, 20, 20);
                age_pie_chart.setDrawEntryLabels(false);

                Legend l = age_pie_chart.getLegend();
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                l.setDrawInside(false);
                l.setEnabled(true);
                l.setTextSize(12f);
                l.setYOffset(-25);

                age_pieEntryList.clear();
                age_pieEntryList.add(new PieEntry(total18_20, "18-20"));
                age_pieEntryList.add(new PieEntry(total21_23, "21-23"));
                age_pieEntryList.add(new PieEntry(total24_26, "24-26"));
                age_pieEntryList.add(new PieEntry(total27_Above, "27 and Above"));

                PieDataSet pieDataSet = new PieDataSet(age_pieEntryList, "");
                int[] colors = new int[10];
                int counter = 0;

                for (int color : ColorTemplate.JOYFUL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                for (int color : ColorTemplate.MATERIAL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                pieDataSet.setColors(colors);
                pieDataSet.setValueTextColor(Color.BLACK);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(true);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if(value == 0.0f){
                            return "";
                        }
                        return String.valueOf((int) value);
                    }
                });
                pieData.setValueTextSize(16f);
                pieData.setValueTextColor(Color.BLACK);

                age_pie_chart.setData(pieData);
                age_pie_chart.invalidate();
                age_pie_chart.animate();
            }
        });
    }

    private void setMonitoringByCourse() {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        Query totalQuery = null, beedQuery = null, bsedQuery = null, bsitETQuery = null, bsitFPSTQuery = null,
                bscsQuery = null, bsfQuery = null, midwiferyQuery = null;

        totalQuery = db.collection("Monitoring")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        beedQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BEED")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        bsedQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BSED")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        bsitETQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BSIT ELECT TECH")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        bsitFPSTQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BSIT FPST")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        bscsQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BSCS")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        bsfQuery = db.collection("Monitoring")
                .whereEqualTo("course", "BSF")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        midwiferyQuery = db.collection("Monitoring")
                .whereEqualTo("course", "MIDWIFERY")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));

        //Get total users
        Task<QuerySnapshot> task1 = totalQuery.get();

        //Get total users from BEED
        Task<QuerySnapshot> task2 = beedQuery.get();

        //Get total users from BSED
        Task<QuerySnapshot> task3 = bsedQuery.get();

        //Get total users from BSIT ELECT TECH
        Task<QuerySnapshot> task4 = bsitETQuery.get();

        //Get total users from BSIT FPST
        Task<QuerySnapshot> task5 = bsitFPSTQuery.get();

        //Get total users from BSCS
        Task<QuerySnapshot> task6 = bscsQuery.get();

        //Get total users from BSF
        Task<QuerySnapshot> task7 = bsfQuery.get();

        //Get total users from MIDWIFERY
        Task<QuerySnapshot> task8 = midwiferyQuery.get();

        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);
        tasks.add(task4);
        tasks.add(task5);
        tasks.add(task6);
        tasks.add(task7);
        tasks.add(task8);

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(new OnCompleteListener<List<Object>>() {
            @Override
            public void onComplete(Task<List<Object>> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    Log.e("App Error", task.getException().getMessage());
                    return;
                }

                QuerySnapshot totalSnapshot = (QuerySnapshot) task.getResult().get(0);
                QuerySnapshot beedSnapshot = (QuerySnapshot) task.getResult().get(1);
                QuerySnapshot bsedSnapshot = (QuerySnapshot) task.getResult().get(2);
                QuerySnapshot bsitFPSTSnapshot = (QuerySnapshot) task.getResult().get(3);
                QuerySnapshot bsitETSnapshot = (QuerySnapshot) task.getResult().get(4);
                QuerySnapshot bscsSnapshot = (QuerySnapshot) task.getResult().get(5);
                QuerySnapshot bsfSnapshot = (QuerySnapshot) task.getResult().get(6);
                QuerySnapshot midwiferySnapshot = (QuerySnapshot) task.getResult().get(7);

                course_pie_chart.setDrawHoleEnabled(true);
                //pie_chart.setUsePercentValues(true);
                course_pie_chart.setEntryLabelTextSize(16f);
                course_pie_chart.setEntryLabelColor(Color.BLACK);
                course_pie_chart.setCenterText("By Course");
                course_pie_chart.setCenterTextSize(16f);
                course_pie_chart.getDescription().setEnabled(false);
                course_pie_chart.setExtraOffsets(20, 20, 20, 20);
                course_pie_chart.setDrawEntryLabels(false);

                Legend l = course_pie_chart.getLegend();
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                l.setDrawInside(false);
                l.setEnabled(true);
                l.setTextSize(12f);
                l.setYOffset(-25);

                course_PieEntryList.clear();
                course_PieEntryList.add(new PieEntry(beedSnapshot.size(), "BEED"));
                course_PieEntryList.add(new PieEntry(bsedSnapshot.size(), "BSED"));
                course_PieEntryList.add(new PieEntry(bsitETSnapshot.size(), "BSIT ELECT TECH"));
                course_PieEntryList.add(new PieEntry(bsitFPSTSnapshot.size(), "BSIT FPST"));
                course_PieEntryList.add(new PieEntry(bscsSnapshot.size(), "BSCS"));
                course_PieEntryList.add(new PieEntry(bsfSnapshot.size(), "BSF"));
                course_PieEntryList.add(new PieEntry(midwiferySnapshot.size(), "MIDWIFERY"));

                PieDataSet pieDataSet = new PieDataSet(course_PieEntryList, "");
                int[] colors = new int[10];
                int counter = 0;

                for (int color : ColorTemplate.JOYFUL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                for (int color : ColorTemplate.MATERIAL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                pieDataSet.setColors(colors);
                pieDataSet.setValueTextColor(Color.BLACK);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(true);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if(value == 0.0f){
                            return "";
                        }
                        return String.valueOf((int) value);
                    }
                });
                pieData.setValueTextSize(16f);
                pieData.setValueTextColor(Color.BLACK);

                course_pie_chart.setData(pieData);
                course_pie_chart.invalidate();
                course_pie_chart.animate();
            }
        });
    }

    private void setMonitoringByPurposeVisit(){
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        Query totalQuery = null, purpose1Query = null, purpose2Query = null, purpose3Query = null, purpose4Query = null,
                purpose5Query = null, purpose6Query = null, purpose7Query = null, purpose8Query = null, purpose9Query = null;

        totalQuery = db.collection("Monitoring")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose1Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To borrow and return books")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose2Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To read journals/periodicals")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose3Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To consult reference books")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose4Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To read general books")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose5Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To complete assignments")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose6Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To prepare for next class")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose7Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To chat with friends")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose8Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "To consult research materials")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));
        purpose9Query = db.collection("Monitoring")
                .whereEqualTo("purposeVisit", "Any others")
                .whereGreaterThanOrEqualTo("timeIn", helper.getStartDate(LocalDate.now()));

        //Get total users
        Task<QuerySnapshot> task1 = totalQuery.get();

        //Get total users with Purpose 1
        Task<QuerySnapshot> task2 = purpose1Query.get();

        //Get total users with Purpose 2
        Task<QuerySnapshot> task3 = purpose2Query.get();

        //Get total users with Purpose 3
        Task<QuerySnapshot> task4 = purpose3Query.get();

        //Get total users with Purpose 4
        Task<QuerySnapshot> task5 = purpose4Query.get();

        //Get total users with Purpose 5
        Task<QuerySnapshot> task6 = purpose5Query.get();

        //Get total users with Purpose 6
        Task<QuerySnapshot> task7 = purpose6Query.get();

        //Get total users with Purpose 7
        Task<QuerySnapshot> task8 = purpose7Query.get();

        //Get total users with Purpose 8
        Task<QuerySnapshot> task9 = purpose8Query.get();

        //Get total users with Purpose 9
        Task<QuerySnapshot> task10 = purpose9Query.get();

        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);
        tasks.add(task4);
        tasks.add(task5);
        tasks.add(task6);
        tasks.add(task7);
        tasks.add(task8);
        tasks.add(task9);
        tasks.add(task10);

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(new OnCompleteListener<List<Object>>() {
            @Override
            public void onComplete(Task<List<Object>> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    Log.e("App Error", task.getException().getMessage());
                    return;
                }

                QuerySnapshot totalSnapshot = (QuerySnapshot) task.getResult().get(0);
                QuerySnapshot purpose1Snapshot = (QuerySnapshot) task.getResult().get(1);
                QuerySnapshot purpose2Snapshot = (QuerySnapshot) task.getResult().get(2);
                QuerySnapshot purpose3Snapshot = (QuerySnapshot) task.getResult().get(3);
                QuerySnapshot purpose4Snapshot = (QuerySnapshot) task.getResult().get(4);
                QuerySnapshot purpose5Snapshot = (QuerySnapshot) task.getResult().get(5);
                QuerySnapshot purpose6Snapshot = (QuerySnapshot) task.getResult().get(6);
                QuerySnapshot purpose7Snapshot = (QuerySnapshot) task.getResult().get(7);
                QuerySnapshot purpose8Snapshot = (QuerySnapshot) task.getResult().get(8);
                QuerySnapshot purpose9Snapshot = (QuerySnapshot) task.getResult().get(9);

                purpose_pie_chart.setDrawHoleEnabled(true);
                //pie_chart.setUsePercentValues(true);
                purpose_pie_chart.setEntryLabelTextSize(16f);
                purpose_pie_chart.setEntryLabelColor(Color.BLACK);
                purpose_pie_chart.setCenterText("By Purpose of Visit");
                purpose_pie_chart.setCenterTextSize(16f);
                purpose_pie_chart.getDescription().setEnabled(false);
                purpose_pie_chart.setExtraOffsets(20, 20, 20, 20);
                purpose_pie_chart.setDrawEntryLabels(false);

                Legend l = purpose_pie_chart.getLegend();
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                l.setDrawInside(false);
                l.setEnabled(true);
                l.setTextSize(12f);

                purpose_PieEntryList.clear();
                purpose_PieEntryList.add(new PieEntry(purpose1Snapshot.size(), "To borrow and return books"));
                purpose_PieEntryList.add(new PieEntry(purpose2Snapshot.size(), "To read journals/periodicals"));
                purpose_PieEntryList.add(new PieEntry(purpose3Snapshot.size(), "To consult reference books"));
                purpose_PieEntryList.add(new PieEntry(purpose4Snapshot.size(), "To read general books"));
                purpose_PieEntryList.add(new PieEntry(purpose5Snapshot.size(), "To complete assignments"));
                purpose_PieEntryList.add(new PieEntry(purpose6Snapshot.size(), "To prepare for next class"));
                purpose_PieEntryList.add(new PieEntry(purpose7Snapshot.size(), "To chat with friends"));
                purpose_PieEntryList.add(new PieEntry(purpose8Snapshot.size(), "To consult research materials"));
                purpose_PieEntryList.add(new PieEntry(purpose9Snapshot.size(), "Any others"));

                PieDataSet pieDataSet = new PieDataSet(purpose_PieEntryList, "");
                int[] colors = new int[10];
                int counter = 0;

                for (int color : ColorTemplate.JOYFUL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                for (int color : ColorTemplate.MATERIAL_COLORS
                ) {
                    colors[counter] = color;
                    counter++;
                }

                pieDataSet.setColors(colors);
                pieDataSet.setValueTextColor(Color.BLACK);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(true);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if(value == 0.0f){
                            return "";
                        }
                        return String.valueOf((int) value);
                    }
                });
                pieData.setValueTextSize(16f);
                pieData.setValueTextColor(Color.BLACK);

                purpose_pie_chart.setData(pieData);
                purpose_pie_chart.invalidate();
                purpose_pie_chart.animate();
            }
        });
    }

}