package com.example.appointable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentHomeFragment extends Fragment {

    private TextView tvGreeting, tvNameOfUser, tvSelectedDate, tvAppointmentIndicators;
    private CalendarView calendarView;
    private RecyclerView rvAppointments;

    private AppointmentAdapter adapter;

    private final List<Appointment> allAppointments = new ArrayList<>();
    private final List<Appointment> filteredAppointments = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Tracks whichever day the user last selected (default = today)
    private Calendar currentSelectedDate = Calendar.getInstance();

    public ParentHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvGreeting              = view.findViewById(R.id.morningNight);
        tvNameOfUser            = view.findViewById(R.id.nameOfUser);
        tvSelectedDate          = view.findViewById(R.id.tvSelectedDate);
        tvAppointmentIndicators = view.findViewById(R.id.tvAppointmentIndicators);
        calendarView            = view.findViewById(R.id.calendarView);
        rvAppointments          = view.findViewById(R.id.rvAppointments);

        setupGreeting();
        initRecycler();

        // ✅ IMPORTANT:
        // REMOVE this line so past dates remain clickable
        // calendarView.setMinDate(System.currentTimeMillis());

        // ✅ Set initial selected date UI + initial filter
        long initialDateMillis = calendarView.getDate(); // calendar's current selected date (usually today)
        currentSelectedDate.setTimeInMillis(initialDateMillis);
        updateSelectedDateLabel(initialDateMillis);

        setupCalendarClick();
        loadAppointments(); // will filter for the currently selected day after fetching

        return view;
    }

    // ---------------- GREETING ----------------

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
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");
                    if (firstName != null && !firstName.isEmpty()) {
                        tvNameOfUser.setText(firstName + "!");
                    }
                });
    }

    // ---------------- RECYCLER ----------------

    private void initRecycler() {
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AppointmentAdapter(
                filteredAppointments,
                new AppointmentAdapter.OnAppointmentActionListener() {

                    @Override
                    public void onCancel(Appointment appt) {
                        showCancelConfirmation(appt);
                    }

                    @Override
                    public void onReschedule(Appointment appt) {
                        showRescheduleDialog(appt);
                    }

                    @Override
                    public void onMoreOptions(Appointment appt, View anchor) {
                        showPopupOptions(appt, anchor);
                    }
                });

        rvAppointments.setAdapter(adapter);
    }

    // ---------------- LOAD FIRESTORE ----------------

    private void loadAppointments() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("studentId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();

                    for (DocumentSnapshot doc : query) {
                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt != null) {
                            // Make sure your Appointment has a valid id.
                            // If your Appointment class doesn't map "id", you can do:
                            // appt.setId(doc.getId());
                            allAppointments.add(appt);
                        }
                    }

                    // ✅ Always refresh list for whichever date is selected
                    filterAppointmentsForDate(currentSelectedDate.getTimeInMillis());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show());
    }

    // ---------------- CALENDAR CLICK ----------------

    private void setupCalendarClick() {
        calendarView.setOnDateChangeListener((view, year, month, day) -> {

            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, day);

            // Normalize time so matching is consistent
            selected.set(Calendar.HOUR_OF_DAY, 0);
            selected.set(Calendar.MINUTE, 0);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            currentSelectedDate = (Calendar) selected.clone();

            updateSelectedDateLabel(selected.getTimeInMillis());
            filterAppointmentsForDate(selected.getTimeInMillis());
        });
    }

    private void updateSelectedDateLabel(long millis) {
        String formatted = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                .format(new Date(millis));
        tvSelectedDate.setText(formatted);
    }

    // ---------------- FILTERING ----------------

    private void filterAppointmentsForDate(long dateMillis) {

        filteredAppointments.clear();

        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(dateMillis);

        // Normalize selected day
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        int count = 0;

        for (Appointment a : allAppointments) {
            try {
                if (a == null || a.getDate() == null) continue;

                String[] parts = a.getDate().split("/");
                if (parts.length < 3) continue;

                int m = Integer.parseInt(parts[0]) - 1;
                int d = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);

                Calendar apptCal = Calendar.getInstance();
                apptCal.set(Calendar.YEAR, y);
                apptCal.set(Calendar.MONTH, m);
                apptCal.set(Calendar.DAY_OF_MONTH, d);

                // Normalize appt day
                apptCal.set(Calendar.HOUR_OF_DAY, 0);
                apptCal.set(Calendar.MINUTE, 0);
                apptCal.set(Calendar.SECOND, 0);
                apptCal.set(Calendar.MILLISECOND, 0);

                if (isSameDay(selected, apptCal)) {
                    filteredAppointments.add(a);
                    count++;
                }

            } catch (Exception ignored) {}
        }

        tvAppointmentIndicators.setText(count + " appointment(s)");

        sortAppointments();
        adapter.notifyDataSetChanged();
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) &&
                (c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR));
    }

    // ---------------- SORT ----------------

    private void sortAppointments() {
        Collections.sort(filteredAppointments, (a1, a2) -> {
            Date d1 = parseDateTime(a1);
            Date d2 = parseDateTime(a2);
            if (d1 == null || d2 == null) return 0;
            return d1.compareTo(d2);
        });
    }

    private Date parseDateTime(Appointment appt) {
        try {
            if (appt == null || appt.getDate() == null || appt.getTime() == null) return null;
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
            return sdf.parse(appt.getDate() + " " + appt.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    // ---------------- OPTIONS MENU ----------------

    private void showPopupOptions(Appointment appt, View anchor) {

        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_options, popup.getMenu());

        String status = appt.getStatus() != null ? appt.getStatus() : "";

        if (status.equalsIgnoreCase("Canceled")) {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(false);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(false);
            popup.getMenu().findItem(R.id.action_remove).setVisible(true);
        } else {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(true);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(true);
            popup.getMenu().findItem(R.id.action_remove).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_reschedule) {
                showRescheduleDialog(appt);
                return true;
            }

            if (item.getItemId() == R.id.action_cancel) {
                showCancelConfirmation(appt);
                return true;
            }

            if (item.getItemId() == R.id.action_remove) {
                showDeleteConfirmation(appt);
                return true;
            }

            return false;
        });

        popup.show();
    }

    // ---------------- DELETE ----------------

    private void showDeleteConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Appointment")
                .setMessage("Remove this appointment permanently?")
                .setPositiveButton("Yes", (d, w) -> removeAppointment(appt))
                .setNegativeButton("No", null)
                .show();
    }

    private void removeAppointment(Appointment appt) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Appointment removed", Toast.LENGTH_SHORT).show();
                    loadAppointments(); // refresh
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
    }

    // ---------------- CANCEL ----------------

    private void showCancelConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes", (d, w) -> updateStatus(appt, "Canceled"))
                .setNegativeButton("No", null)
                .show();
    }

    // ---------------- RESCHEDULE ----------------

    private void showRescheduleDialog(Appointment appt) {

        View view = getLayoutInflater().inflate(R.layout.dialog_reschedule, null);

        EditText etNewDate = view.findViewById(R.id.etNewDate);
        EditText etNewTime = view.findViewById(R.id.etNewTime);

        etNewDate.setOnClickListener(v -> pickDate(etNewDate));
        etNewTime.setOnClickListener(v -> pickTime(etNewTime));

        new AlertDialog.Builder(getContext())
                .setTitle("Reschedule Appointment")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {

                    String newDate = etNewDate.getText().toString().trim();
                    String newTime = etNewTime.getText().toString().trim();

                    if (newDate.isEmpty() || newTime.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Pick new date and time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateReschedule(appt, newDate, newTime);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDate(EditText et) {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                getContext(),
                (picker, y, m, d) -> et.setText((m + 1) + "/" + d + "/" + y),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickTime(EditText et) {
        Calendar c = Calendar.getInstance();

        new TimePickerDialog(
                getContext(),
                (picker, h, m) -> {
                    String ampm = (h >= 12) ? "PM" : "AM";
                    int hour = (h % 12 == 0) ? 12 : h % 12;
                    et.setText(hour + ":" + String.format(Locale.getDefault(), "%02d", m) + " " + ampm);
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
        ).show();
    }

    // ---------------- FIRESTORE UPDATES ----------------

    private void updateStatus(Appointment appt, String status) {

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update("status", status)
                .addOnSuccessListener(a -> loadAppointments())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void updateReschedule(Appointment appt, String newDate, String newTime) {

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update(
                        "date", newDate,
                        "time", newTime,
                        "status", "Rescheduled"
                )
                .addOnSuccessListener(a -> loadAppointments())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Reschedule failed", Toast.LENGTH_SHORT).show());
    }
}
