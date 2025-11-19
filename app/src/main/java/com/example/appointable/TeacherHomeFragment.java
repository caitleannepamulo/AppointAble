package com.example.appointable;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvNameOfUser;
    private TextView tvSelectedDate;
    private RecyclerView rvAppointments;
    private CalendarView calendarView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppointmentAdapter adapter;
    private List<Appointment> allAppointments = new ArrayList<>();
    private List<Appointment> appointmentList = new ArrayList<>();

    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

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
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        rvAppointments = view.findViewById(R.id.rvAppointments);
        calendarView = view.findViewById(R.id.calendarView);

        setGreetingText();
        loadUserName();

        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList, this::showAppointmentDetails);
        rvAppointments.setAdapter(adapter);

        loadStaticAppointments();

        long todayDate = normalizeDateMillis(System.currentTimeMillis());
        filterAppointmentsByDate(todayDate);
        updateSelectedDateLabel(todayDate);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            long selectedDate = getDateInMillis(year, month, dayOfMonth);
            filterAppointmentsByDate(selectedDate);
            updateSelectedDateLabel(selectedDate);
        });
    }

    private void setGreetingText() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 12) {
            tvGreeting.setText("Good Morning,");
        } else if (hour >= 12 && hour < 18) {
            tvGreeting.setText("Good Afternoon,");
        } else {
            tvGreeting.setText("Good Evening,");
        }
    }

    private void loadUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            tvNameOfUser.setText("Unknown User!");
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this::handleUserDocument)
                .addOnFailureListener(e -> tvNameOfUser.setText("Unknown User!"));
    }

    private void handleUserDocument(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot != null && documentSnapshot.exists()) {
            String firstName = documentSnapshot.getString("firstName");

            if (firstName != null && !firstName.trim().isEmpty()) {
                String trimmed = firstName.trim();
                String formatted =
                        trimmed.substring(0, 1).toUpperCase() +
                                trimmed.substring(1).toLowerCase();

                tvNameOfUser.setText(formatted + "!");
            } else {
                tvNameOfUser.setText("Unknown User!");
            }
        } else {
            tvNameOfUser.setText("Unknown User!");
        }
    }

    private void loadStaticAppointments() {
        allAppointments.clear();

        long nov19_2025 = getDateInMillis(2025, Calendar.NOVEMBER, 19);
        long nov20_2025 = getDateInMillis(2025, Calendar.NOVEMBER, 20);
        long nov22_2025 = getDateInMillis(2025, Calendar.NOVEMBER, 22);
        long nov23_2025 = getDateInMillis(2025, Calendar.NOVEMBER, 23);
        long nov24_2025 = getDateInMillis(2025, Calendar.NOVEMBER, 24);

        allAppointments.add(new Appointment("joshua pre", "Speech Therapy", "9:00 AM", "Pending", nov19_2025));
        allAppointments.add(new Appointment("maria santos", "Occupational Therapy", "10:30 AM", "Completed", nov20_2025));
        allAppointments.add(new Appointment("liam cruz", "Physical Therapy", "1:00 PM", "Rescheduled", nov23_2025));
        allAppointments.add(new Appointment("alyssa dela rosa", "Reading Intervention", "3:00 PM", "Cancelled", nov24_2025));
        allAppointments.add(new Appointment("kevin tan", "Behavioral Therapy", "8:15 AM", "Pending", nov22_2025));

        for (int i = 0; i < allAppointments.size(); i++) {
            Appointment a = allAppointments.get(i);
            String name = a.getChildName();
            if (name != null && !name.trim().isEmpty()) {
                String trimmed = name.trim();
                String formatted =
                        trimmed.substring(0, 1).toUpperCase() +
                                trimmed.substring(1).toLowerCase();
                allAppointments.set(i, new Appointment(
                        formatted,
                        a.getService(),
                        a.getTime(),
                        a.getStatus(),
                        a.getDateMillis()
                ));
            }
        }
    }

    private void filterAppointmentsByDate(long selectedDateMillis) {
        appointmentList.clear();

        for (Appointment a : allAppointments) {
            if (isSameDay(a.getDateMillis(), selectedDateMillis)) {
                appointmentList.add(a);
            }
        }

        Collections.sort(appointmentList, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment a1, Appointment a2) {
                try {
                    Date d1 = timeFormat.parse(a1.getTime());
                    Date d2 = timeFormat.parse(a2.getTime());
                    if (d1 == null || d2 == null) return 0;
                    return d1.compareTo(d2);
                } catch (ParseException e) {
                    return 0;
                }
            }
        });

        adapter.notifyDataSetChanged();
    }

    private void updateSelectedDateLabel(long dateMillis) {
        long today = normalizeDateMillis(System.currentTimeMillis());
        if (isSameDay(today, dateMillis)) {
            tvSelectedDate.setText("Today");
        } else {
            SimpleDateFormat df = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            tvSelectedDate.setText(df.format(new Date(dateMillis)));
        }
    }

    private boolean isSameDay(long date1Millis, long date2Millis) {
        long n1 = normalizeDateMillis(date1Millis);
        long n2 = normalizeDateMillis(date2Millis);
        return n1 == n2;
    }

    private long normalizeDateMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getDateInMillis(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void showAppointmentDetails(Appointment appt) {
        if (getContext() == null) return;

        String message = "Child: " + appt.getChildName() +
                "\nService: " + appt.getService() +
                "\nTime: " + appt.getTime() +
                "\nStatus: " + appt.getStatus();

        new AlertDialog.Builder(getContext())
                .setTitle("Appointment Details")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
