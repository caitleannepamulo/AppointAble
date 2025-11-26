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
    private final List<Appointment> appointmentList = new ArrayList<>();

    // Master list from Firestore (all statuses)
    private final List<Appointment> allAppointments = new ArrayList<>();

    // Current status filter: "Pending", "Canceled", "Rescheduled", "Completed"
    // (Pending filter will include both Pending + Accepted)
    private String currentStatusFilter = "Pending";

    // Layouts for the status cards (for click + highlight)
    private MaterialCardView layoutCanceled, layoutRescheduled, layoutPending, layoutCompleted;

    private List<String> teacherNamesList = new ArrayList<>();
    private List<String> teacherIdList = new ArrayList<>();

    private TextView tvCanceledCount, tvRescheduledCount, tvCompletedCount, tvPendingCount;

    public AppointmentsFragment() {}

    // -----------------------------------------------------------
    // LIFECYCLE
    // -----------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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

        // Status card layouts
        layoutCanceled    = root.findViewById(R.id.layoutCanceled);
        layoutRescheduled = root.findViewById(R.id.layoutRescheduled);
        layoutPending     = root.findViewById(R.id.layoutPending);
        layoutCompleted   = root.findViewById(R.id.layoutCompleted);

        // Click listeners for filtering
        layoutPending.setOnClickListener(v -> setStatusFilter("Pending"));      // Pending + Accepted
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
    // LOAD APPOINTMENTS FROM FIRESTORE
    // -----------------------------------------------------------
    private void loadAppointments() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("studentId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {

                    allAppointments.clear();      // master list
                    appointmentList.clear();      // visible list

                    int canceled = 0, rescheduled = 0, pending = 0, completed = 0;

                    for (DocumentSnapshot doc : query) {

                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt == null) continue;

                        allAppointments.add(appt);

                        String statusRaw = appt.getStatus();
                        String status = statusRaw != null
                                ? statusRaw.toLowerCase()
                                : "";

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
                                // Pending bucket includes Pending + Accepted
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
                });
    }

    // When user taps one of the cards
    private void setStatusFilter(String status) {
        currentStatusFilter = status;
        applyStatusFilter();
        highlightSelectedStatus();
    }

    // Build appointmentList based on currentStatusFilter
    private void applyStatusFilter() {
        appointmentList.clear();

        for (Appointment a : allAppointments) {
            String st = a.getStatus();

            if (st == null) continue;

            if ("Pending".equalsIgnoreCase(currentStatusFilter)) {
                // Pending filter shows both Pending AND Accepted
                if (st.equalsIgnoreCase("Pending") || st.equalsIgnoreCase("Accepted")) {
                    appointmentList.add(a);
                }
            } else {
                // Other filters: exact match
                if (st.equalsIgnoreCase(currentStatusFilter)) {
                    appointmentList.add(a);
                }
            }
        }

        // ------- SORT BY NEAREST DATE/TIME (soonest at the top) -------
        Collections.sort(appointmentList, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment a1, Appointment a2) {
                Date d1 = parseAppointmentDateTime(a1);
                Date d2 = parseAppointmentDateTime(a2);

                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;  // nulls go to bottom
                if (d2 == null) return -1;

                return d1.compareTo(d2);   // ascending: earliest first
            }
        });
        // --------------------------------------------------------------

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // Simple highlight by changing alpha on the selected card
    private void highlightSelectedStatus() {
        if (layoutPending == null) return; // safety if views not ready yet

        // Reset
        layoutPending.setAlpha(1f);
        layoutCanceled.setAlpha(1f);
        layoutRescheduled.setAlpha(1f);
        layoutCompleted.setAlpha(1f);

        // Dim the selected one a bit (or adjust as you like)
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

        String dateStr = appt.getDate(); // e.g. "3/25/2025"
        String timeStr = appt.getTime(); // e.g. "2:30 PM"

        if (dateStr == null || timeStr == null) return null;

        String combined = dateStr + " " + timeStr;

        // Pattern must match how you format date/time in pickDate/pickTime
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy h:mm a", Locale.getDefault());
        try {
            return sdf.parse(combined);
        } catch (ParseException e) {
            e.printStackTrace();
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

        // ---- Appointment Type Spinner with hint ----
        String[] types = {
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
                // Disable the first item (hint)
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;
                if (position == 0) {
                    tv.setTextColor(0xFF9E9E9E); // gray
                } else {
                    tv.setTextColor(0xFF000000); // black
                }
                return v;
            }
        };
        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection(0);

        // ---- Teachers (hint added inside loadTeachers) ----
        loadTeachers(spinnerTeacher);

        etDate.setOnClickListener(v -> pickDate(etDate));
        etTime.setOnClickListener(v -> pickTime(etTime));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {

            int typePos = spinnerType.getSelectedItemPosition();
            int teacherPos = spinnerTeacher.getSelectedItemPosition();

            // Validate type
            if (typePos == 0) {
                Toast.makeText(getContext(), "Please select appointment type", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate teacher
            if (teacherPos == 0) {
                Toast.makeText(getContext(), "Please select teacher", Toast.LENGTH_SHORT).show();
                return;
            }

            String service = spinnerType.getSelectedItem().toString();
            String date = etDate.getText().toString();
            String time = etTime.getText().toString();

            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(getContext(), "Pick date & time", Toast.LENGTH_SHORT).show();
                return;
            }

            // teacherPos - 1 because index 0 is "--Select Teacher--"
            String teacherName = teacherNamesList.get(teacherPos - 1);
            String teacherId   = teacherIdList.get(teacherPos - 1);

            saveAppointment(
                    service,
                    date,
                    time,
                    teacherName,
                    teacherId
            );

            dialog.dismiss();
        });

        dialog.show();
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
                    String ampm = h >= 12 ? "PM" : "AM";
                    int hr = (h % 12 == 0 ? 12 : h % 12);
                    et.setText(hr + ":" + String.format("%02d", m) + " " + ampm);
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
        ).show();
    }

    // -----------------------------------------------------------
    // SAVE APPOINTMENT
    // -----------------------------------------------------------
    private void saveAppointment(String service, String date, String time,
                                 String teacherName, String teacherId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(getContext(), "Pick date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = user.getUid();

        db.collection("users").document(studentId)
                .get()
                .addOnSuccessListener(doc -> {

                    String childName = doc.getString("firstName") + " " + doc.getString("lastName");
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
                                loadAppointments();
                            });
                });
    }

    // -----------------------------------------------------------
    // OPTIONS MENU
    // -----------------------------------------------------------
    private void showPopupOptions(Appointment appt, View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_options, popup.getMenu());

        // Get status safely
        String status = appt.getStatus() != null ? appt.getStatus() : "";

        // If status is "Canceled", only show "Remove Appointment"
        if (status.equalsIgnoreCase("Canceled")) {
            popup.getMenu().findItem(R.id.action_reschedule).setVisible(false);
            popup.getMenu().findItem(R.id.action_cancel).setVisible(false);
            popup.getMenu().findItem(R.id.action_remove).setVisible(true);
        } else {
            // For non-canceled: show Reschedule + Cancel, hide Remove
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
    // RESCHEDULE
    // -----------------------------------------------------------
    private void showRescheduleDialog(Appointment appt) {

        View view = getLayoutInflater().inflate(R.layout.dialog_reschedule, null);

        EditText etNewDate = view.findViewById(R.id.etNewDate);
        EditText etNewTime = view.findViewById(R.id.etNewTime);
        EditText etComment = view.findViewById(R.id.etComment);

        etNewDate.setOnClickListener(v -> pickDate(etNewDate));
        etNewTime.setOnClickListener(v -> pickTime(etNewTime));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Reschedule Appointment")
                .setView(view)
                .setPositiveButton("Save", null)   // override later
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newDate = etNewDate.getText().toString().trim();
                String newTime = etNewTime.getText().toString().trim();
                String comment = etComment.getText().toString().trim();

                boolean hasError = false;

                if (newDate.isEmpty()) {
                    etNewDate.setError("Please select a new date");
                    hasError = true;
                } else {
                    etNewDate.setError(null);
                }

                if (newTime.isEmpty()) {
                    etNewTime.setError("Please select a new time");
                    hasError = true;
                } else {
                    etNewTime.setError(null);
                }

                if (comment.isEmpty()) {
                    etComment.setError("Please provide a comment / reason");
                    hasError = true;
                } else {
                    etComment.setError(null);
                }

                if (hasError) {
                    return; // keep dialog open
                }

                updateReschedule(appt, newDate, newTime, comment);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateStatus(Appointment appt, String newStatus) {
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appt.getId())
                .update("status", newStatus)
                .addOnSuccessListener(a -> loadAppointments());
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
                .addOnSuccessListener(a -> loadAppointments());
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
                        Toast.makeText(getContext(), "Failed to remove", Toast.LENGTH_SHORT).show()
                );
    }

    // -----------------------------------------------------------
    // LOAD TEACHERS
    // -----------------------------------------------------------
    private void loadTeachers(Spinner spinnerTeacher) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", "Sped Teacher")
                .get()
                .addOnSuccessListener(query -> {

                    teacherNamesList.clear();
                    teacherIdList.clear();

                    for (DocumentSnapshot doc : query) {
                        String name = doc.getString("firstName") + " " + doc.getString("lastName");
                        teacherNamesList.add(name);
                        teacherIdList.add(doc.getId());
                    }

                    // Build display list with hint at index 0
                    List<String> displayList = new ArrayList<>();
                    displayList.add("--Select Teacher--");
                    displayList.addAll(teacherNamesList);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            displayList
                    ) {
                        @Override
                        public boolean isEnabled(int position) {
                            // Disable first item (hint)
                            return position != 0;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            View v = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) v;
                            if (position == 0) {
                                tv.setTextColor(0xFF9E9E9E); // gray
                            } else {
                                tv.setTextColor(0xFF000000); // black
                            }
                            return v;
                        }
                    };

                    spinnerTeacher.setAdapter(adapter);
                    spinnerTeacher.setSelection(0);
                });
    }
}
