package com.example.appointable;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleModel> list;

    public ScheduleAdapter(List<ScheduleModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleModel model = list.get(position);

        holder.tvChildName.setText(model.getChildName());
        holder.tvService.setText(model.getService());
        holder.tvTime.setText(model.getTime());

        if (model.getStatus() == 1) {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_complete);
        } else if (model.getStatus() == 2) {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_cancelled);
        } else {
            holder.ivCalendar.setImageResource(R.drawable.ic_calendar_pending);
        }

        // click whole card → details
        holder.itemView.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle(model.getChildName())
                    .setMessage("Service: " + model.getService() +
                            "\nTime: " + model.getTime())
                    .setPositiveButton("OK", null)
                    .show();
        });

        holder.ivCalendar.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Mark appointment")
                    .setMessage("Do you confirm that this appointment is already completed?")
                    .setNegativeButton("No", (dialog, which) -> {
                        model.setStatus(2);
                        notifyItemChanged(holder.getAdapterPosition());
                        Toast.makeText(v.getContext(),
                                "Marked as cancelled / not attended.",
                                Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton("Yes", (dialog, which) -> {
                        model.setStatus(1);
                        notifyItemChanged(holder.getAdapterPosition());
                        Toast.makeText(v.getContext(),
                                "Marked as completed.",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvChildName, tvService, tvTime;
        ImageView ivCalendar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvService = itemView.findViewById(R.id.tvService);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivCalendar = itemView.findViewById(R.id.ivCalendar);
        }
    }
}
