package com.example.appointable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Schedule_TeacherFragment extends Fragment {

    private RecyclerView rvAllAppointments;
    private TextView tvRemainingCount, tvCompletedCount;
    private ImageView ivSort;
    private CardView cardRemainingAppointments, cardCompletedAppointments;
    private final List<ScheduleModel> weekAppointments = new ArrayList<>();
    private final List<ScheduleModel> workingAppointments = new ArrayList<>();
    private final List<ScheduleModel> displayedAppointments = new ArrayList<>();
    private ScheduleAdapter adapter;
    private int currentIndex = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isAscending = true;
    private int statusFilter = 0;

    public Schedule_TeacherFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule_teacher, container, false);

        tvRemainingCount = view.findViewById(R.id.tvRemainingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        rvAllAppointments = view.findViewById(R.id.rvAllAppointments);
        ivSort = view.findViewById(R.id.ivSort);
        cardRemainingAppointments = view.findViewById(R.id.cardRemainingAppointments);
        cardCompletedAppointments = view.findViewById(R.id.cardCompletedAppointments);

        setDummyWeekAppointments();
        setStatsFromData();
        setupRecyclerView();
        setupSortButton();
        setupFilterCards();

        return view;
    }

    private void setDummyWeekAppointments() {
        weekAppointments.clear();

        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "9:00 AM", 9 * 60));
        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "11:00 AM", 11 * 60));
        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "2:00 PM", 14 * 60));
        weekAppointments.add(new ScheduleModel("Maria Lois", "Occupational Therapy", "3:30 PM", 15 * 60 + 30));
        weekAppointments.add(new ScheduleModel("Kevin Santos", "Behavioral Therapy", "4:00 PM", 16 * 60));

        ScheduleModel done1 = new ScheduleModel("Ana Cruz", "Speech Therapy", "8:00 AM", 8 * 60);
        done1.setStatus(1);
        weekAppointments.add(done1);

        ScheduleModel done2 = new ScheduleModel("John Lim", "Occupational Therapy", "10:30 AM", 10 * 60 + 30);
        done2.setStatus(1);
        weekAppointments.add(done2);

        ScheduleModel cancelled = new ScheduleModel("Ella Mae", "Speech Therapy", "5:30 PM", 17 * 60 + 30);
        cancelled.setStatus(2);
        weekAppointments.add(cancelled);
    }

    private void setStatsFromData() {
        int remaining = 0;
        int completed = 0;

        for (ScheduleModel m : weekAppointments) {
            if (m.getStatus() == 1) {
                completed++;
            } else if (m.getStatus() == 0) {
                remaining++;
            }
        }

        tvRemainingCount.setText(String.valueOf(remaining));
        tvCompletedCount.setText(String.valueOf(completed));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvAllAppointments.setLayoutManager(layoutManager);

        adapter = new ScheduleAdapter(displayedAppointments);
        rvAllAppointments.setAdapter(adapter);

        applyFilterSortAndReset();

        rvAllAppointments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        currentIndex < workingAppointments.size()) {
                    loadNextPage();
                }
            }
        });
    }

    private void setupSortButton() {
        ivSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            applyFilterSortAndReset();
        });
    }

    private void setupFilterCards() {
        cardRemainingAppointments.setOnClickListener(v -> {
            statusFilter = 0;
            applyFilterSortAndReset();
        });

        cardCompletedAppointments.setOnClickListener(v -> {
            statusFilter = 1;
            applyFilterSortAndReset();
        });
    }

    private void applyFilterSortAndReset() {
        workingAppointments.clear();

        for (ScheduleModel m : weekAppointments) {
            if (statusFilter == 0 && m.getStatus() == 0) {
                workingAppointments.add(m);
            } else if (statusFilter == 1 && m.getStatus() == 1) {
                workingAppointments.add(m);
            }
        }

        Collections.sort(workingAppointments, new Comparator<ScheduleModel>() {
            @Override
            public int compare(ScheduleModel a, ScheduleModel b) {
                if (isAscending) {
                    return Integer.compare(a.getSortTimeMinutes(), b.getSortTimeMinutes());
                } else {
                    return Integer.compare(b.getSortTimeMinutes(), a.getSortTimeMinutes());
                }
            }
        });

        displayedAppointments.clear();
        adapter.notifyDataSetChanged();
        currentIndex = 0;
        loadNextPage();
    }

    private void loadNextPage() {
        if (currentIndex >= workingAppointments.size()) return;

        isLoading = true;

        int nextLimit = Math.min(currentIndex + pageSize, workingAppointments.size());
        List<ScheduleModel> subList = workingAppointments.subList(currentIndex, nextLimit);

        int start = displayedAppointments.size();
        displayedAppointments.addAll(subList);
        adapter.notifyItemRangeInserted(start, subList.size());

        currentIndex = nextLimit;
        isLoading = false;
    }
}
