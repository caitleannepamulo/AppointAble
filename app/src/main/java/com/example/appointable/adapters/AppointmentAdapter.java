package com.example.appointable.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.R;
import com.example.appointable.models.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointmentList;

    public AppointmentAdapter(List<Appointment> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appt = appointmentList.get(position);

        holder.tvTeacherName.setText(appt.getTeacherName());
        holder.tvService.setText(appt.getService());
        holder.tvDate.setText(appt.getDate());
        holder.tvTime.setText(appt.getTime());
        holder.tvStatus.setText(appt.getStatus());
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public void updateList(List<com.example.appointable.Appointment> allAppointments) {
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        TextView tvTeacherName, tvService, tvDate, tvTime, tvStatus;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            tvService   = itemView.findViewById(R.id.tvService);
            tvDate      = itemView.findViewById(R.id.tvDate);
            tvTime      = itemView.findViewById(R.id.tvTime);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
        }
    }
}
