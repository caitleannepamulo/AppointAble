package com.example.appointable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ParentHomeFragment extends Fragment {

    public ParentHomeFragment() {}
    private TextView tvGreeting, tvNameOfUser;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//        Inflating the layout
        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

//        Accessing the view by view.findViewById
        tvGreeting = view.findViewById(R.id.morningNight);     // Make sure these IDs exist
        tvNameOfUser = view.findViewById(R.id.nameOfUser);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupGreeting();

        return view;
    }

    private void setupGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) tvGreeting.setText("Good Morning,");
        else if (hour < 18) tvGreeting.setText("Good Afternoon,");
        else tvGreeting.setText("Good Evening,");

        loadUserName();
    }

    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            tvNameOfUser.setText("Unknown User!");
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");

                    if (firstName != null && !firstName.isEmpty()) {
                        String formatted = firstName.substring(0, 1).toUpperCase() +
                                firstName.substring(1).toLowerCase();
                        tvNameOfUser.setText(formatted + "!");
                    } else {
                        tvNameOfUser.setText("Unknown User!");
                    }
                })
                .addOnFailureListener(e ->
                        tvNameOfUser.setText("Unknown User!"));
    }
}