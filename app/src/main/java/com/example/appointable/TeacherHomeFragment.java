package com.example.appointable;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.adapters.AppointmentAdapter;
import com.example.appointable.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TeacherHomeFragment extends Fragment {

    private TextView tvGreeting, tvNameOfUser, tvTodayTitle;
    private ImageView ivTodayCalendar, ivQuoteImage;

    private RecyclerView rvToday;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AppointmentAdapter appointmentAdapter;

    private final List<Appointment> allAppointments = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_teacher_home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvGreeting = view.findViewById(R.id.goodMorning);
        tvNameOfUser = view.findViewById(R.id.nameOfUser);
        tvTodayTitle = view.findViewById(R.id.tvTodayTitle);

        ivTodayCalendar = view.findViewById(R.id.ivTodayCalendar);
        ivQuoteImage = view.findViewById(R.id.ivQuoteImage);

        rvToday = view.findViewById(R.id.rvToday);

        setupGreeting();
        setupRecyclerViews();
        loadTeacherAppointments();
        setupCalendarPicker();
        showRandomQuoteImage();
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
                    if (firstName != null) {
                        tvNameOfUser.setText(firstName + "!");
                    }
                });
    }

    private void setupRecyclerViews() {
        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));

        appointmentAdapter = new AppointmentAdapter(
                new ArrayList<>(),
                new AppointmentAdapter.OnAppointmentActionListener() {
                    @Override public void onCancel(Appointment appt) {}
                    @Override public void onReschedule(Appointment appt) {}
                    @Override public void onMoreOptions(Appointment appt, View anchor) {}
                }
        );

        rvToday.setAdapter(appointmentAdapter);
    }

    private void loadTeacherAppointments() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String teacherId = user.getUid();

        db.collection("appointments")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();

                    for (DocumentSnapshot doc : query) {
                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt != null) allAppointments.add(appt);
                    }

                    Calendar today = Calendar.getInstance();
                    filterAppointments(today);
                });
    }

    private void filterAppointments(Calendar selected) {

        List<Appointment> filtered = new ArrayList<>();

        for (Appointment a : allAppointments) {
            if (a.getDate() == null) continue;

            String[] parts = a.getDate().split("/");
            if (parts.length != 3) continue;

            int m = Integer.parseInt(parts[0]) - 1;
            int d = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);

            Calendar apptDate = Calendar.getInstance();
            apptDate.set(y, m, d);

            if (sameDay(apptDate, selected)) filtered.add(a);
        }

        appointmentAdapter.updateList(filtered);
        tvTodayTitle.setText(dateFormat.format(selected.getTime()));
    }

    private boolean sameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private void setupCalendarPicker() {
        ivTodayCalendar.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, y, m, d) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(y, m, d);
                filterAppointments(selected);
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void showRandomQuoteImage() {
        int[] images = {
                R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
                R.drawable.pic4, R.drawable.pic5, R.drawable.pic6,
                R.drawable.pic7, R.drawable.pic8, R.drawable.pic9,
                R.drawable.pic10, R.drawable.pic11, R.drawable.pic12
        };

        ivQuoteImage.setImageResource(images[new Random().nextInt(images.length)]);
    }
}
