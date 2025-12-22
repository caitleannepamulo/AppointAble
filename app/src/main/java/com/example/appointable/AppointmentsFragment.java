package com.example.appointable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class AppointmentsFragment extends Fragment {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;

    private final List<Appointment> appointmentList = new ArrayList<Appointment>();
    private final List<Appointment> allAppointments = new ArrayList<Appointment>();

    private String currentStatusFilter = "Pending";

    private MaterialCardView layoutCanceled, layoutRescheduled, layoutPending, layoutCompleted;

    private final List<String> teacherNamesList = new ArrayList<String>();
    private final List<String> teacherIdList = new ArrayList<String>();

    private TextView tvCanceledCount, tvRescheduledCount, tvCompletedCount, tvPendingCount;

    // Date format used across dialogs
    private final SimpleDateFormat dateOnlySdf = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());

    public AppointmentsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_parent_appointment_fragment, container, false);

        initViews(root);
        initRecycler();
        loadAppointments();

        return root;
    }

    private void initViews(View root) {

        tvCanceledCount    = root.findViewById(R.id.tvCanceledCount);
        tvRescheduledCount = root.findViewById(R.id.tvRescheduledCount);
        tvCompletedCount   = root.findViewById(R.id.tvCompletedCount);
        tvPendingCount     = root.findViewById(R.id.tvPending);

        rvAppointments = root.findViewById(R.id.rvAppointments);

        layoutCanceled    = root.findViewById(R.id.layoutCanceled);
        layoutRescheduled = root.findViewById(R.id.layoutRescheduled);
        layoutPending     = root.findViewById(R.id.layoutPending);
        layoutCompleted   = root.findViewById(R.id.layoutCompleted);

        layoutPending.setOnClickListener(v -> setStatusFilter("Pending"));
        layoutCanceled.setOnClickListener(v -> setStatusFilter("Canceled"));
        layoutRescheduled.setOnClickListener(v -> setStatusFilter("Rescheduled"));
        layoutCompleted.setOnClickListener(v -> setStatusFilter("Completed"));

        FloatingActionButton fab = root.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddAppointmentDialog());
    }

    private void initRecycler() {
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList, new AppointmentAdapter.OnAppointmentActionListener() {
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

    // -----------------------------------------------------------
    // LOAD APPOINTMENTS
    // -----------------------------------------------------------
    private void loadAppointments() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("studentId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();
                    appointmentList.clear();

                    int canceled = 0, rescheduled = 0, pending = 0, completed = 0;

                    for (DocumentSnapshot doc : query) {

                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt == null) continue;

                        if (appt.getId() == null || appt.getId().trim().isEmpty()) {
                            appt.setId(doc.getId());
                        }

                        allAppointments.add(appt);

                        String status = appt.getStatus() == null ? "" : appt.getStatus().toLowerCase();

                        switch (status) {
                            case "canceled":
                                canceled++;
                                break;
                            case "rescheduled":
                                rescheduled++;
                                break;
                            case "completed":
                                completed++;
                                break;
                            case "pending":
                            case "accepted":
                                pending++;
                                break;
                        }
                    }

                    tvCanceledCount.setText(String.valueOf(canceled));
                    tvRescheduledCount.setText(String.valueOf(rescheduled));
                    tvPendingCount.setText(String.valueOf(pending));
                    tvCompletedCount.setText(String.valueOf(completed));

                    applyStatusFilter();
                    highlightSelectedStatus();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void setStatusFilter(String status) {
        currentStatusFilter = status;
        applyStatusFilter();
        highlightSelectedStatus();
    }

    private void applyStatusFilter() {
        appointmentList.clear();

        for (Appointment a : allAppointments) {
            String st = a.getStatus();
            if (st == null) continue;

            if ("Pending".equalsIgnoreCase(currentStatusFilter)) {
                if (st.equalsIgnoreCase("Pending") || st.equalsIgnoreCase("Accepted")) {
                    appointmentList.add(a);
                }
            } else {
                if (st.equalsIgnoreCase(currentStatusFilter)) {
                    appointmentList.add(a);
                }
            }
        }

        Collections.sort(appointmentList, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment a1, Appointment a2) {
                Date d1 = parseAppointmentDateTime(a1);
                Date d2 = parseAppointmentDateTime(a2);

                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;

                return d1.compareTo(d2);
            }
        });

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void highlightSelectedStatus() {
        if (layoutPending == null) return;

        layoutPending.setAlpha(1f);
        layoutCanceled.setAlpha(1f);
        layoutRescheduled.setAlpha(1f);
        layoutCompleted.setAlpha(1f);

        switch (currentStatusFilter) {
            case "Pending":
                layoutPending.setAlpha(0.7f);
                break;
            case "Canceled":
                layoutCanceled.setAlpha(0.7f);
                break;
            case "Rescheduled":
                layoutRescheduled.setAlpha(0.7f);
                break;
            case "Completed":
                layoutCompleted.setAlpha(0.7f);
                break;
        }
    }

    private Date parseAppointmentDateTime(Appointment appt) {
        if (appt == null) return null;

        String dateStr = appt.getDate();
        String timeStr = appt.getTime();

        if (dateStr == null || timeStr == null) return null;

        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
        try {
            return sdf.parse(dateStr + " " + timeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    // -----------------------------------------------------------
    // ADD APPOINTMENT DIALOG
    // -----------------------------------------------------------
    private void showAddAppointmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_appointment, null);
        builder.setView(view);

        Spinner spinnerType = view.findViewById(R.id.spinnerType);
        Spinner spinnerTeacher = view.findViewById(R.id.spinnerTeacher);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etTime = view.findViewById(R.id.etTime);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveAppointment);

        String[] types = new String[]{
                "--Select Appointment Type--",
                "Admin Officer",
                "OT Associates",
                "Sped Teacher",
                "Sped Coordinator",
                "Occupational Therapist"
        };

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                types
        ) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;
                tv.setTextColor(position == 0 ? 0xFF9E9E9E : 0xFF000000);
                return v;
            }
        };

        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection(0);

        setTeacherSpinnerWaitingType(spinnerTeacher);
        btnSave.setEnabled(false);

        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {

                if (position == 0) {
                    setTeacherSpinnerWaitingType(spinnerTeacher);
                    teacherNamesList.clear();
                    teacherIdList.clear();
                    btnSave.setEnabled(false);
                    return;
                }

                String selectedRole = spinnerType.getSelectedItem().toString();

                setTeacherSpinnerLoading(spinnerTeacher);
                btnSave.setEnabled(false);

                loadTeachersByRole(spinnerTeacher, btnSave, selectedRole);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        etDate.setOnClickListener(v -> pickFutureDate(etDate));
        etTime.setOnClickListener(v -> pickTimeRange(etTime, "Select Time"));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {

            int typePos = spinnerType.getSelectedItemPosition();
            if (typePos <= 0) {
                Toast.makeText(getContext(), "Please select appointment type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (teacherNamesList.isEmpty() || teacherIdList.isEmpty()) {
                Toast.makeText(getContext(), "No staff available for this type", Toast.LENGTH_SHORT).show();
                return;
            }

            int teacherPos = spinnerTeacher.getSelectedItemPosition();
            if (teacherPos <= 0) {
                Toast.makeText(getContext(), "Please select teacher", Toast.LENGTH_SHORT).show();
                return;
            }

            int index = teacherPos - 1;
            if (index < 0 || index >= teacherNamesList.size() || index >= teacherIdList.size()) {
                Toast.makeText(getContext(), "Teacher selection invalid. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            String service = spinnerType.getSelectedItem().toString();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(getContext(), "Pick date & time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extra safety: prevent saving past date
            if (!isSelectedDateTodayOrFuture(date)) {
                Toast.makeText(getContext(), "Past dates are not allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            String teacherName = teacherNamesList.get(index);
            String teacherId = teacherIdList.get(index);

            saveAppointment(service, date, time, teacherName, teacherId, dialog);
        });

        dialog.show();
    }

    private void setTeacherSpinnerWaitingType(Spinner spinnerTeacher) {
        List<String> list = new ArrayList<String>();
        list.add("--Select Appointment Type First--");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                list
        ) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };

        spinnerTeacher.setAdapter(adapter);
        spinnerTeacher.setSelection(0);
    }

    private void setTeacherSpinnerLoading(Spinner spinnerTeacher) {
        List<String> list = new ArrayList<String>();
        list.add("--Loading Staff--");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                list
        ) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };

        spinnerTeacher.setAdapter(adapter);
        spinnerTeacher.setSelection(0);
    }

    private void setTeacherSpinnerError(Spinner spinnerTeacher) {
        List<String> list = new ArrayList<String>();
        list.add("--Failed to Load Staff--");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                list
        ) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };

        spinnerTeacher.setAdapter(adapter);
        spinnerTeacher.setSelection(0);
    }

    private void loadTeachersByRole(Spinner spinnerTeacher, MaterialButton btnSave, String role) {

        teacherNamesList.clear();
        teacherIdList.clear();
        btnSave.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(query -> {

                    for (DocumentSnapshot doc : query) {
                        String first = doc.getString("firstName");
                        String last = doc.getString("lastName");
                        if (first == null) first = "";
                        if (last == null) last = "";

                        String name = (first + " " + last).trim();
                        if (name.isEmpty()) name = role;

                        teacherNamesList.add(name);
                        teacherIdList.add(doc.getId());
                    }

                    List<String> displayList = new ArrayList<String>();
                    displayList.add("--Select Teacher--");
                    displayList.addAll(teacherNamesList);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            displayList
                    ) {
                        @Override
                        public boolean isEnabled(int position) {
                            return position != 0;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            View v = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) v;
                            tv.setTextColor(position == 0 ? 0xFF9E9E9E : 0xFF000000);
                            return v;
                        }
                    };

                    spinnerTeacher.setAdapter(adapter);
                    spinnerTeacher.setSelection(0);

                    if (teacherNamesList.isEmpty()) {
                        Toast.makeText(getContext(), "No staff found for: " + role, Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(false);
                    } else {
                        btnSave.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load staff: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(false);
                    setTeacherSpinnerError(spinnerTeacher);
                });
    }

    // -----------------------------------------------------------
    // DATE PICKER: DISABLE PAST DATES
    // -----------------------------------------------------------
    private void pickFutureDate(EditText et) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                getContext(),
                (picker, y, m, d) -> et.setText((m + 1) + "/" + d + "/" + y),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        // Disable past dates (today allowed)
        dp.getDatePicker().setMinDate(getStartOfTodayMillis());

        dp.show();
    }

    private long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private boolean isSelectedDateTodayOrFuture(String dateStr) {
        try {
            Date selected = dateOnlySdf.parse(dateStr);
            if (selected == null) return false;

            Calendar selCal = Calendar.getInstance();
            selCal.setTime(selected);
            selCal.set(Calendar.HOUR_OF_DAY, 0);
            selCal.set(Calendar.MINUTE, 0);
            selCal.set(Calendar.SECOND, 0);
            selCal.set(Calendar.MILLISECOND, 0);

            return selCal.getTimeInMillis() >= getStartOfTodayMillis();
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------
    // TIME PICKER: 6AM to 6PM ONLY
    // -----------------------------------------------------------
    private void pickTimeRange(EditText et, String title) {

        Calendar now = Calendar.getInstance();
        int startHour = 6;   // 6 AM
        int endHour = 18;    // 6 PM

        // Default to nearest valid hour inside range
        int defaultHour = now.get(Calendar.HOUR_OF_DAY);
        if (defaultHour < startHour) defaultHour = startHour;
        if (defaultHour > endHour) defaultHour = startHour;

        int defaultMin = now.get(Calendar.MINUTE);

        TimePickerDialog tp = new TimePickerDialog(
                getContext(),
                (picker, hourOfDay, minute) -> {

                    // Block anything outside 6am-6pm (6pm allowed only at 6:00)
                    if (hourOfDay < startHour || hourOfDay > endHour || (hourOfDay == endHour && minute > 0)) {
                        Toast.makeText(getContext(), "Time must be between 6:00 AM and 6:00 PM", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String ampm = hourOfDay >= 12 ? "PM" : "AM";
                    int hr = (hourOfDay % 12 == 0 ? 12 : hourOfDay % 12);
                    et.setText(hr + ":" + String.format(Locale.getDefault(), "%02d", minute) + " " + ampm);
                },
                defaultHour,
                defaultMin,
                false
        );

        tp.setTitle(title);
        tp.show();
    }

    // -----------------------------------------------------------
    // SAVE APPOINTMENT
    // -----------------------------------------------------------
    private void saveAppointment(String service, String date, String time,
                                 String teacherName, String teacherId,
                                 AlertDialog dialogToClose) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = user.getUid();

        db.collection("users").document(studentId)
                .get()
                .addOnSuccessListener(doc -> {

                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");
                    if (first == null) first = "";
                    if (last == null) last = "";

                    String childName = (first + " " + last).trim();
                    if (childName.isEmpty()) childName = "Student";

                    String id = db.collection("appointments").document().getId();

                    Appointment appt = new Appointment(
                            id, studentId, childName,
                            teacherId, teacherName, service,
                            date, time, "Pending"
                    );

                    db.collection("appointments").document(id)
                            .set(appt)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(), "Appointment Sent", Toast.LENGTH_SHORT).show();
                                if (dialogToClose != null && dialogToClose.isShowing()) {
                                    dialogToClose.dismiss();
                                }
                                loadAppointments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "User fetch failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // -----------------------------------------------------------
    // OPTIONS MENU
    // -----------------------------------------------------------
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
            int id = item.getItemId();

            if (id == R.id.action_reschedule) {
                showRescheduleDialog(appt);
                return true;
            }

            if (id == R.id.action_cancel) {
                showCancelConfirmation(appt);
                return true;
            }

            if (id == R.id.action_remove) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove Appointment")
                        .setMessage("Remove this appointment permanently?")
                        .setPositiveButton("Yes", (d, w) -> removeAppointment(appt))
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void showCancelConfirmation(Appointment appt) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Appointment")
                .setMessage("Cancel this appointment?")
                .setPositiveButton("Yes", (d, w) -> updateStatus(appt, "Canceled"))
                .setNegativeButton("No", null)
                .show();
    }

    // -----------------------------------------------------------
    // RESCHEDULE (DISABLE PAST DATES + TIME RANGE)
    // -----------------------------------------------------------
    private void showRescheduleDialog(Appointment appt) {

        View view = getLayoutInflater().inflate(R.layout.dialog_reschedule, null);

        EditText etNewDate = view.findViewById(R.id.etNewDate);
        EditText etNewTime = view.findViewById(R.id.etNewTime);
        EditText etComment = view.findViewById(R.id.etComment);

        etNewDate.setOnClickListener(v -> pickFutureDate(etNewDate));
        etNewTime.setOnClickListener(v -> pickTimeRange(etNewTime, "Select New Time"));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Reschedule Appointment")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newDate = etNewDate.getText().toString().trim();
            String newTime = etNewTime.getText().toString().trim();
            String comment = etComment.getText().toString().trim();

            boolean hasError = false;

            if (newDate.isEmpty()) { etNewDate.setError("Please select a new date"); hasError = true; }
            else etNewDate.setError(null);

            if (newTime.isEmpty()) { etNewTime.setError("Please select a new time"); hasError = true; }
            else etNewTime.setError(null);

            if (comment.isEmpty()) { etComment.setError("Please provide a comment / reason"); hasError = true; }
            else etComment.setError(null);

            if (hasError) return;

            if (!isSelectedDateTodayOrFuture(newDate)) {
                Toast.makeText(getContext(), "Past dates are not allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            updateReschedule(appt, newDate, newTime, comment);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void updateStatus(Appointment appt, String newStatus) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update("status", newStatus)
                .addOnSuccessListener(a -> loadAppointments())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateReschedule(Appointment appt, String newDate, String newTime, String comment) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update(
                        "date", newDate,
                        "time", newTime,
                        "status", "Rescheduled",
                        "rescheduleComment", comment
                )
                .addOnSuccessListener(a -> loadAppointments())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Reschedule failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // -----------------------------------------------------------
    // REMOVE APPOINTMENT
    // -----------------------------------------------------------
    private void removeAppointment(Appointment appt) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Appointment removed", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
