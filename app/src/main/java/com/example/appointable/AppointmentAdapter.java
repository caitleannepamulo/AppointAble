package com.example.appointable;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appt);
    }

    private List<Appointment> appointmentList;
    private OnAppointmentClickListener listener;

    public AppointmentAdapter(List<Appointment> appointmentList,
                              OnAppointmentClickListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appt = appointmentList.get(position);

        holder.childName.setText(appt.getChildName());
        holder.service.setText(appt.getService());
        holder.time.setText(appt.getTime());
        holder.status.setText(appt.getStatus());

        String status = appt.getStatus() != null ? appt.getStatus().toLowerCase().trim() : "";
        int color;

        switch (status) {
            case "completed":
                color = Color.parseColor("#4CAF50");
                break;
            case "pending":
                color = Color.parseColor("#2196F3");
                break;
            case "rescheduled":
                color = Color.parseColor("#FFC107");
                break;
            case "cancelled":
            case "canceled":
                color = Color.parseColor("#F44336");
                break;
            default:
                color = Color.parseColor("#2196F3");
                break;
        }

        if (holder.status.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable bg =
                    (android.graphics.drawable.GradientDrawable) holder.status.getBackground().mutate();
            bg.setColor(color);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppointmentClick(appt);
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView childName, service, time, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            childName = itemView.findViewById(R.id.tvChildName);
            service = itemView.findViewById(R.id.tvService);
            time = itemView.findViewById(R.id.tvTime);
            status = itemView.findViewById(R.id.tvStatus);
        }
    }
}
