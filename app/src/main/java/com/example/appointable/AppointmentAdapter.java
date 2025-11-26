package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.R;
import com.example.appointable.Appointment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointmentList;
    private OnAppointmentActionListener listener;

    // Stores which items are expanded
    private Map<String, Boolean> expandedMap = new HashMap<>();

    // Listener for cancel, reschedule, and options menu
    public interface OnAppointmentActionListener {
        void onCancel(Appointment appt);
        void onReschedule(Appointment appt);
        void onMoreOptions(Appointment appt, View anchor);
    }

    public AppointmentAdapter(List<Appointment> appointmentList, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
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
        holder.tvDate.setText(formatDisplayDate(appt.getDate()));
        holder.tvTime.setText(appt.getTime());
        holder.tvStatus.setText(appt.getStatus());

        // Add this INSIDE the method
        if (listener != null) {
            holder.btnOptions.setOnClickListener(v ->
                    listener.onMoreOptions(appt, v)
            );
        } else {
            holder.btnOptions.setVisibility(View.GONE);
        }

        String comment = appt.getRescheduleComment();
        holder.tvComment.setText("Comment: " + (comment == null || comment.isEmpty() ? "None" : comment));

        String id = appt.getId();
        boolean expanded = expandedMap.getOrDefault(id, false);

        // -----------------------------------------
        // SHOW DROPDOWN ONLY WHEN STATUS = RESCHEDULED
        // -----------------------------------------
        if (appt.getStatus().equalsIgnoreCase("Rescheduled")) {

            // Show arrow
            holder.imgArrow.setVisibility(View.VISIBLE);

            // Apply current expansion state
            holder.layoutDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.imgArrow.setRotation(expanded ? 180f : 0f);

            // Toggle when clicked
            View.OnClickListener toggle = v -> {
                boolean newState = !expanded;
                expandedMap.put(id, newState);
                notifyItemChanged(holder.getAdapterPosition());
            };

            holder.layoutRoot.setOnClickListener(toggle);
            holder.imgArrow.setOnClickListener(toggle);

        } else {
            // -----------------------------------------
            // FOR NON-RESCHEDULED ITEMS
            // -----------------------------------------
            holder.imgArrow.setVisibility(View.GONE);   // Hide arrow
            holder.layoutDetails.setVisibility(View.GONE); // Hide expanded area
            expandedMap.put(id, false);                // Force collapsed
        }
    }


    // Convert stored date "11/26/2025" → "November 26, 2025"
    private String formatDisplayDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";

        SimpleDateFormat input = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        try {
            Date date = input.parse(dateStr);
            return output.format(date);
        } catch (ParseException e) {
            return dateStr; // fallback
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    // Update list externally
    public void updateList(List<Appointment> newList) {
        appointmentList.clear();
        appointmentList.addAll(newList);
        notifyDataSetChanged();
    }

    // ViewHolder
    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        LinearLayout layoutRoot, layoutDetails;
        TextView tvTeacherName, tvService, tvDate, tvTime, tvStatus, tvComment;
        ImageView btnOptions, imgArrow;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutRoot = itemView.findViewById(R.id.layoutRoot);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);

            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            tvService = itemView.findViewById(R.id.tvService);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvComment = itemView.findViewById(R.id.tvComment);

            btnOptions = itemView.findViewById(R.id.btnOptions);
            imgArrow = itemView.findViewById(R.id.imgArrow);
        }
    }
}