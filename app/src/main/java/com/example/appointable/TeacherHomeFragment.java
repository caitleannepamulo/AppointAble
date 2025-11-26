package com.example.appointable;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;        // <-- added
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

    private AppointmentTeacherAdapter appointmentTeacherAdapter;
    private SummaryAdapter summaryAdapter;

    private final List<AppointmentTeacher> allAppointmentTeachers = new ArrayList<>();
    private final List<SummaryItem> summaryItems = new ArrayList<>();

    private boolean isAscending = true;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    private Calendar selectedDate = Calendar.getInstance();

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

        tvTodayTitle.setText(dateFormat.format(selectedDate.getTime()));

        loadAppointmentsFromFirestore();
        generateDummySummary();
        setupCalendarPicker();
        setupSummarySorting();
        showRandomQuoteImage();
    }

    // ------------------ greeting + user name ------------------

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
                .addOnFailureListener(e -> tvNameOfUser.setText("Unknown User!"));
    }

    // ------------------ RecyclerViews ------------------

    private void setupRecyclerViews() {
        rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentTeacherAdapter = new AppointmentTeacherAdapter(new ArrayList<>());
        rvToday.setAdapter(appointmentTeacherAdapter);

        rvSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        summaryAdapter = new SummaryAdapter(new ArrayList<>());
        rvSummary.setAdapter(summaryAdapter);
    }

    // ------------------ Firestore: load appointments with specific statuses ------------------

    private void loadAppointmentsFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = user.getUid();

        db.collection("appointments")
                .whereEqualTo("teacherId", teacherId)
                .whereIn("status", Arrays.asList(
                        "Accepted",
                        "Rescheduled",
                        "Canceled",
                        "Completed"
                ))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allAppointmentTeachers.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String childName = doc.getString("childName");
                        String service = doc.getString("service");
                        String timeStr = doc.getString("time");
                        String dateStr = doc.getString("date");
                        String statusStr = doc.getString("status");

                        if (childName == null) childName = "";
                        if (service == null) service = "";
                        if (timeStr == null) timeStr = "";
                        if (statusStr == null) statusStr = "";

                        Calendar dateCal = Calendar.getInstance();
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                dateCal.setTime(dbDateFormat.parse(dateStr));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        allAppointmentTeachers.add(
                                new AppointmentTeacher(childName, service, timeStr, dateCal, statusStr)
                        );
                    }

                    updateAppointmentsForSelectedDate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to load appointments.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAppointmentsForSelectedDate() {
        List<AppointmentTeacher> filtered = new ArrayList<>();

        for (AppointmentTeacher a : allAppointmentTeachers) {
            if (sameDay(a.getDate(), selectedDate)) filtered.add(a);
        }

        appointmentTeacherAdapter.updateList(filtered);
        tvTodayTitle.setText(dateFormat.format(selectedDate.getTime()));
    }

    private boolean sameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    // ------------------ Summary (dummy for now) ------------------

    private void generateDummySummary() {
        summaryItems.clear();

        summaryItems.add(new SummaryItem("Kevin Tan", "Behavior Therapy", 22));
        summaryItems.add(new SummaryItem("Joshua Pre", "Speech Therapy", 37));

        summaryAdapter.updateItems(summaryItems);
    }

    // ------------------ Date picker ------------------

    private void setupCalendarPicker() {
        ivTodayCalendar.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();

            new DatePickerDialog(
                    getContext(),
                    (view, year, month, day) -> {
                        selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, day);

                        updateAppointmentsForSelectedDate();
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ------------------ Summary sorting ------------------

    private void setupSummarySorting() {
        ivSummaryIcon.setOnClickListener(v -> {
            isAscending = !isAscending;
            summaryAdapter.sortByProgress(isAscending);
            ivSummaryIcon.setRotation(isAscending ? 0f : 180f);
        });
    }

    // ------------------ Random quote image ------------------

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
