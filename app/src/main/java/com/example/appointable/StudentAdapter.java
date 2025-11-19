package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private final List<StudentModel> list;

    public StudentAdapter(List<StudentModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentModel m = list.get(position);

        holder.tvID.setText(m.studentNumber);
        holder.tvName.setText(m.getFullName());
        holder.tvGrade.setText(m.grade);
        holder.tvStatus.setText(m.status);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvID, tvName, tvGrade, tvStatus, tvView, tvEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvID = itemView.findViewById(R.id.tvID);
            tvName = itemView.findViewById(R.id.tvName);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvView = itemView.findViewById(R.id.tvView);
            tvEdit = itemView.findViewById(R.id.tvEdit);

            tvView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    StudentModel m = list.get(pos);
                    showViewDialog(itemView, m);
                }
            });

            tvEdit.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    StudentModel m = list.get(pos);
                    showEditDialog(itemView, m, pos);
                }
            });
        }

        private void showViewDialog(View itemView, StudentModel m) {
            View dialogView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.dialog_student_view, null, false);

            EditText etViewID = dialogView.findViewById(R.id.etViewID);
            EditText etViewName = dialogView.findViewById(R.id.etViewName);
            EditText etViewGrade = dialogView.findViewById(R.id.etViewGrade);
            Spinner spinnerViewStatus = dialogView.findViewById(R.id.spinnerViewStatus);
            Button btnRegister = dialogView.findViewById(R.id.btnRegister);

            etViewID.setText(m.studentNumber);
            etViewName.setText(m.getFullName());
            etViewGrade.setText(m.grade);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    itemView.getContext(),
                    R.array.student_status_array,
                    android.R.layout.simple_spinner_dropdown_item
            );
            spinnerViewStatus.setAdapter(adapter);

            int index = adapter.getPosition(m.status);
            spinnerViewStatus.setSelection(index);

            AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                    .setView(dialogView)
                    .create();

            btnRegister.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        private void showEditDialog(View itemView, StudentModel m, int position) {
            View dialogView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.dialog_student_edit, null, false);

            EditText etFirstname = dialogView.findViewById(R.id.etFirstname);
            EditText etLastname = dialogView.findViewById(R.id.etLastname);
            EditText etMiddlename = dialogView.findViewById(R.id.etMiddlename);
            EditText etSuffix = dialogView.findViewById(R.id.etSuffix);
            EditText etStudentNumber = dialogView.findViewById(R.id.etStudentNumber);
            EditText etGrades = dialogView.findViewById(R.id.etGrades);
            Spinner spStatus = dialogView.findViewById(R.id.spStatus);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);

            etFirstname.setText(m.firstName);
            etLastname.setText(m.lastName);
            etMiddlename.setText(m.middleName);
            etSuffix.setText(m.suffix);
            etStudentNumber.setText(m.studentNumber);
            etGrades.setText(m.grade);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    itemView.getContext(),
                    R.array.student_status_array,
                    android.R.layout.simple_spinner_dropdown_item
            );
            spStatus.setAdapter(adapter);
            spStatus.setSelection(adapter.getPosition(m.status));

            AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnUpdate.setOnClickListener(v -> {
                m.firstName = etFirstname.getText().toString();
                m.lastName = etLastname.getText().toString();
                m.middleName = etMiddlename.getText().toString();
                m.suffix = etSuffix.getText().toString();
                m.studentNumber = etStudentNumber.getText().toString();
                m.grade = etGrades.getText().toString();
                m.status = spStatus.getSelectedItem().toString();

                notifyItemChanged(position);
                dialog.dismiss();
            });

            dialog.show();
        }
    }
}
