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
import com.example.appointable.SummaryAdapter;
import com.example.appointable.Appointment;
import com.example.appointable.SummaryItem;
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
    private ImageView ivTodayCalendar, ivSummaryIcon, ivQuoteImage;

    private RecyclerView rvToday, rvSummary;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AppointmentAdapter appointmentAdapter;
    private SummaryAdapter summaryAdapter;

    private final List<Appointment> allAppointments = new ArrayList<>();
    private final List<SummaryItem> summaryItems = new ArrayList<>();

    private boolean isAscending = true;

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
        ivSummaryIcon = view.findViewById(R.id.ivSummaryIcon);
        ivQuoteImage = view.findViewById(R.id.ivQuoteImage);

        rvToday = view.findViewById(R.id.rvToday);
        rvSummary = view.findViewById(R.id.rvSummary);

        setupGreeting();
        setupRecyclerViews();
        generateDummyData();
        setupCalendarPicker();
        setupSummarySorting();
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

                    if (firstName != null && !firstName.isEmpty()) {
                        String formatted = firstName.substring(0, 1).toUpperCase() +
                                firstName.substring(1).toLowerCase();
                        tvNameOfUser.setText(formatted + "!");
                    }
                })
                .addOnFailureListener(e ->
                        tvNameOfUser.setText("Unknown User!"));
    }

    private void setupRecyclerViews() {
        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentAdapter = new AppointmentAdapter(new ArrayList<>());
        rvToday.setAdapter(appointmentAdapter);

        rvSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        summaryAdapter = new SummaryAdapter(new ArrayList<>());
        rvSummary.setAdapter(summaryAdapter);
    }

    private void generateDummyData() {
        Calendar today = Calendar.getInstance();

        allAppointments.add(new Appointment("Kevin Tan", "Behavioral Therapy", "8:15 AM", today, "Pending"));
        allAppointments.add(new Appointment("Joshua Pre", "Speech Therapy", "9:00 AM", today, "Pending"));

        summaryItems.add(new SummaryItem("Kevin Tan", "Behavior Therapy", 22));
        summaryItems.add(new SummaryItem("Joshua Pre", "Speech Therapy", 37));

        appointmentAdapter.updateList(allAppointments);
        summaryAdapter.updateItems(summaryItems);

        tvTodayTitle.setText(dateFormat.format(today.getTime()));
    }

    private void setupCalendarPicker() {
        ivTodayCalendar.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();

            new DatePickerDialog(getContext(), (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day);

                tvTodayTitle.setText(dateFormat.format(selected.getTime()));

                filterAppointments(selected);

            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void filterAppointments(Calendar selected) {
        List<Appointment> filtered = new ArrayList<>();

        for (Appointment a : allAppointments) {
            if (sameDay(a.getDate(), selected)) filtered.add(a);
        }

        appointmentAdapter.updateList(filtered);
    }

    private boolean sameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private void setupSummarySorting() {
        ivSummaryIcon.setOnClickListener(v -> {
            isAscending = !isAscending;

            summaryAdapter.sortByProgress(isAscending);
            ivSummaryIcon.setRotation(isAscending ? 0f : 180f);
        });
    }

    private void showRandomQuoteImage() {
        int[] images = {
                R.drawable.pic1,
                R.drawable.pic2,
                R.drawable.pic3,
                R.drawable.pic4,
                R.drawable.pic5,
                R.drawable.pic6,
                R.drawable.pic7,
                R.drawable.pic8,
                R.drawable.pic9,
                R.drawable.pic10,
                R.drawable.pic11,
                R.drawable.pic12
        };

        ivQuoteImage.setImageResource(
                images[new Random().nextInt(images.length)]
        );
    }
}