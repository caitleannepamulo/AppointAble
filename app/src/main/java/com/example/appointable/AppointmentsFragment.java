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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.adapters.AppointmentAdapter;
import com.example.appointable.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AppointmentsFragment extends Fragment {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private final List<Appointment> appointmentList = new ArrayList<>();

    private List<String> teacherNamesList = new ArrayList<>();
    private List<String> teacherIdList = new ArrayList<>();

    private TextView tvCanceledCount, tvRescheduledCount, tvCompletedCount;

    public AppointmentsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_parent_appointment_fragment, container, false);

        // Counters
        tvCanceledCount     = root.findViewById(R.id.tvCanceledCount);
        tvRescheduledCount  = root.findViewById(R.id.tvRescheduledCount);
        tvCompletedCount    = root.findViewById(R.id.tvCompletedCount);

        // RecyclerView
        rvAppointments = root.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(appointmentList);
        rvAppointments.setAdapter(adapter);

        // Fab
        FloatingActionButton fab = root.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddAppointmentDialog());

        // Load data now
        loadAppointments();

        return root;
    }

    // -----------------------------------------------------------
    // LOAD TEACHERS
    // -----------------------------------------------------------
    private void loadTeachers(Spinner spinnerTeacher) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
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

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            teacherNamesList
                    );

                    spinnerTeacher.setAdapter(adapter);
                });
    }

    // -----------------------------------------------------------
    // DIALOG
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

        spinnerType.setAdapter(new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Speech Therapy", "Consultation", "OT Session"}
        ));

        loadTeachers(spinnerTeacher);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(
                    getContext(),
                    (picker, y, m, d) -> etDate.setText((m + 1) + "/" + d + "/" + y),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(
                    getContext(),
                    (picker, h, m) -> {
                        String ampm = h >= 12 ? "PM" : "AM";
                        int hr = (h % 12 == 0) ? 12 : h % 12;
                        etTime.setText(hr + ":" + String.format("%02d", m) + " " + ampm);
                    },
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    false
            ).show();
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {

            int pos = spinnerTeacher.getSelectedItemPosition();
            if (pos < 0) {
                Toast.makeText(getContext(), "Please select a teacher", Toast.LENGTH_SHORT).show();
                return;
            }

            saveAppointment(
                    spinnerType.getSelectedItem().toString(),
                    etDate.getText().toString(),
                    etTime.getText().toString(),
                    teacherNamesList.get(pos),
                    teacherIdList.get(pos)
            );

            dialog.dismiss();
        });

        dialog.show();
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

                    appointmentList.clear();

                    int canceled = 0, rescheduled = 0, completed = 0;

                    for (DocumentSnapshot doc : query) {

                        Appointment appt = doc.toObject(Appointment.class);
                        if (appt == null) continue;

                        appointmentList.add(appt);

                        String status = appt.getStatus().toLowerCase();

                        if (status.equals("canceled"))     canceled++;
                        if (status.equals("rescheduled"))  rescheduled++;
                        if (status.equals("completed"))    completed++;
                    }

                    tvCanceledCount.setText("" + canceled);
                    tvRescheduledCount.setText("" + rescheduled);
                    tvCompletedCount.setText("" + completed);

                    adapter.notifyDataSetChanged();
                });
    }

    // -----------------------------------------------------------
    // SAVE APPOINTMENT
    // -----------------------------------------------------------
    private void saveAppointment(String service, String date, String time,
                                 String teacherName, String teacherId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String studentId = user.getUid();

        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(getContext(), "Please pick date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(studentId)
                .get()
                .addOnSuccessListener(doc -> {

                    String childName = doc.getString("firstName") + " " + doc.getString("lastName");

                    String id = db.collection("appointments").document().getId();

                    Appointment appt = new Appointment(
                            id,
                            studentId,
                            childName,
                            teacherId,
                            teacherName,
                            service,
                            date,
                            time,
                            "Pending"
                    );

                    db.collection("appointments").document(id)
                            .set(appt)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(), "Appointment Sent!", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            });
                });
    }
}
