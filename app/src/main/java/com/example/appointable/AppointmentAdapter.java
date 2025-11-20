package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.R;
import com.example.appointable.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> items;

    public AppointmentAdapter(List<Appointment> items) {
        this.items = items;
    }

    public void updateList(List<Appointment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
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
        Appointment item = items.get(position);

        holder.tvChildName.setText(item.getChildName());
        holder.tvService.setText(item.getService());
        holder.tvTime.setText(item.getTime());
        holder.tvStatus.setText(item.getStatus());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName, tvService, tvTime, tvStatus;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvService = itemView.findViewById(R.id.tvService);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
