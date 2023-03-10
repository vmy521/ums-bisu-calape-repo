package com.bisu.ums_bisucalapelibrary.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bisu.ums_bisucalapelibrary.EnrollFaceActivity;
import com.bisu.ums_bisucalapelibrary.Helper;
import com.bisu.ums_bisucalapelibrary.Listener;
import com.bisu.ums_bisucalapelibrary.R;
import com.bisu.ums_bisucalapelibrary.adapter.UserAdapter;
import com.bisu.ums_bisucalapelibrary.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfilingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfilingFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView rv;
    private TextView tv_noresult, tv_total;
    private UserAdapter adapter;
    private FirebaseFirestore db;
    private String search = "";
    private Helper helper;

    public ProfilingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfilingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfilingFragment newInstance(String param1, String param2) {
        ProfilingFragment fragment = new ProfilingFragment();
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
        return inflater.inflate(R.layout.fragment_profiling, container, false);
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

        setOptionsMenu();
    }

    private void load(){
        Query query;

        if(!search.isEmpty()){
            query = db.collection("User")
                    .orderBy("fullName")
                    .whereEqualTo("fullName", search);
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
                    tv_noresult.setVisibility(View.INVISIBLE);
                }else{
                    tv_noresult.setVisibility(View.VISIBLE);
                }
                System.out.println(String.valueOf(total));
                tv_total.setText(String.valueOf(total));
            }
        };

        adapter = new UserAdapter(getContext(), options, listener);
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
                    menuInflater.inflate(R.menu.menu_profiling, menu);
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
                    case R.id.add:
                        startActivity(new Intent(getContext(), EnrollFaceActivity.class));
                        break;
                }
                return true;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

}