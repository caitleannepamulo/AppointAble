package com.example.appointable;

import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Schedule_TeacherFragment extends Fragment {

    private RecyclerView rvAllAppointments;
    private TextView tvRemainingCount, tvCompletedCount;
    private ImageView ivSort;
    private CardView cardRemainingAppointments, cardCompletedAppointments;
    private TextView tvMonday, tvTuesday, tvWednesday, tvThursday, tvFriday, tvSaturday, tvSunday;
    private final List<ScheduleModel> weekAppointments = new ArrayList<>();
    private final List<ScheduleModel> workingAppointments = new ArrayList<>();
    private final List<ScheduleModel> displayedAppointments = new ArrayList<>();
    private ScheduleAdapter adapter;
    private int currentIndex = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isAscending = true;
    private int statusFilter = 0;
    private int selectedDay = 0;
    private int weekOffset = 0;
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public Schedule_TeacherFragment() {
    }

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

        tvMonday = view.findViewById(R.id.tvMonday);
        tvTuesday = view.findViewById(R.id.tvTuesday);
        tvWednesday = view.findViewById(R.id.tvWednesday);
        tvThursday = view.findViewById(R.id.tvThursday);
        tvFriday = view.findViewById(R.id.tvFriday);
        tvSaturday = view.findViewById(R.id.tvSaturday);
        tvSunday = view.findViewById(R.id.tvSunday);

        setDummyWeekAppointments();
        setStatsFromData();
        setupRecyclerView();
        setupSortButton();
        setupFilterCards();
        setupDayClickListeners();

        setWeekDayLabels();
        autoSelectToday();

        setupSwipeGesture(view);

        return view;
    }

    private void setupSwipeGesture(View rootView) {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {

                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        changeWeek(-1);
                    } else {
                        changeWeek(1);
                    }
                    return true;
                }
                return false;
            }
        });

        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void changeWeek(int delta) {
        weekOffset += delta;

        selectedDay = 0;
        clearDayHighlights();

        setWeekDayLabels();

        if (weekOffset == 0) {
            autoSelectToday();
        } else {
            applyFilterSortAndReset();
        }
    }

    private void setWeekDayLabels() {
        Calendar realToday = Calendar.getInstance();

        Calendar monday = Calendar.getInstance();
        monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        monday.add(Calendar.WEEK_OF_YEAR, weekOffset);

        Calendar tuesday = (Calendar) monday.clone();
        tuesday.add(Calendar.DAY_OF_MONTH, 1);

        Calendar wednesday = (Calendar) monday.clone();
        wednesday.add(Calendar.DAY_OF_MONTH, 2);

        Calendar thursday = (Calendar) monday.clone();
        thursday.add(Calendar.DAY_OF_MONTH, 3);

        Calendar friday = (Calendar) monday.clone();
        friday.add(Calendar.DAY_OF_MONTH, 4);

        Calendar saturday = (Calendar) monday.clone();
        saturday.add(Calendar.DAY_OF_MONTH, 5);

        Calendar sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        setDayLabel(tvMonday, "Mon", monday, realToday, dateFormat, 1);
        setDayLabel(tvTuesday, "Tue", tuesday, realToday, dateFormat, 2);
        setDayLabel(tvWednesday, "Wed", wednesday, realToday, dateFormat, 3);
        setDayLabel(tvThursday, "Thu", thursday, realToday, dateFormat, 4);
        setDayLabel(tvFriday, "Fri", friday, realToday, dateFormat, 5);
        setDayLabel(tvSaturday, "Sat", saturday, realToday, dateFormat, 6);
        setDayLabel(tvSunday, "Sun", sunday, realToday, dateFormat, 7);
    }

    private void setDayLabel(TextView tv,
                             String dayShortName,
                             Calendar dayCal,
                             Calendar realToday,
                             SimpleDateFormat dateFormat,
                             int dayOfWeek) {

        int countForDay = getAppointmentsCountForDay(dayOfWeek);

        String dateText;
        if (isSameDay(dayCal, realToday) && weekOffset == 0) {
            dateText = "Today";
        } else {
            dateText = dateFormat.format(dayCal.getTime());
        }

        tv.setText(dayShortName + " (" + countForDay + ")\n" + dateText);
    }

    private int getAppointmentsCountForDay(int dayOfWeek) {
        int count = 0;
        for (ScheduleModel m : weekAppointments) {
            if (m.getDayOfWeek() == dayOfWeek) {
                count++;
            }
        }
        return count;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void autoSelectToday() {
        if (weekOffset != 0) {
            return;
        }

        Calendar today = Calendar.getInstance();
        int mapped = mapCalendarDayToCustom(today.get(Calendar.DAY_OF_WEEK));
        selectedDay = mapped;

        applyFilterSortAndReset();

        switch (mapped) {
            case 1:
                highlightSelectedDay(tvMonday);
                break;
            case 2:
                highlightSelectedDay(tvTuesday);
                break;
            case 3:
                highlightSelectedDay(tvWednesday);
                break;
            case 4:
                highlightSelectedDay(tvThursday);
                break;
            case 5:
                highlightSelectedDay(tvFriday);
                break;
            case 6:
                highlightSelectedDay(tvSaturday);
                break;
            case 7:
                highlightSelectedDay(tvSunday);
                break;
        }
    }

    private int mapCalendarDayToCustom(int calDay) {
        switch (calDay) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
            default:
                return 0;
        }
    }

    private void setDummyWeekAppointments() {
        weekAppointments.clear();

        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "9:00 AM", 9 * 60, 1));
        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "11:00 AM", 11 * 60, 1));

        weekAppointments.add(new ScheduleModel("Kevin Santos", "Behavioral Therapy", "4:00 PM", 16 * 60, 2));

        weekAppointments.add(new ScheduleModel("Maria Lois", "Occupational Therapy", "3:30 PM", 15 * 60 + 30, 3));

        weekAppointments.add(new ScheduleModel("Joshua Pre", "Speech Therapy", "2:00 PM", 14 * 60, 4));

        ScheduleModel done1 = new ScheduleModel("Ana Cruz", "Speech Therapy", "8:00 AM", 8 * 60, 5);
        done1.setStatus(1);
        weekAppointments.add(done1);

        ScheduleModel done2 = new ScheduleModel("John Lim", "Occupational Therapy", "10:30 AM", 10 * 60 + 30, 6);
        done2.setStatus(1);
        weekAppointments.add(done2);

        ScheduleModel cancelled = new ScheduleModel("Ella Mae", "Speech Therapy", "5:30 PM", 17 * 60 + 30, 7);
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

    private void setupDayClickListeners() {
        tvMonday.setOnClickListener(v -> {
            selectedDay = 1;
            applyFilterSortAndReset();
            highlightSelectedDay(tvMonday);
        });

        tvTuesday.setOnClickListener(v -> {
            selectedDay = 2;
            applyFilterSortAndReset();
            highlightSelectedDay(tvTuesday);
        });

        tvWednesday.setOnClickListener(v -> {
            selectedDay = 3;
            applyFilterSortAndReset();
            highlightSelectedDay(tvWednesday);
        });

        tvThursday.setOnClickListener(v -> {
            selectedDay = 4;
            applyFilterSortAndReset();
            highlightSelectedDay(tvThursday);
        });

        tvFriday.setOnClickListener(v -> {
            selectedDay = 5;
            applyFilterSortAndReset();
            highlightSelectedDay(tvFriday);
        });

        tvSaturday.setOnClickListener(v -> {
            selectedDay = 6;
            applyFilterSortAndReset();
            highlightSelectedDay(tvSaturday);
        });

        tvSunday.setOnClickListener(v -> {
            selectedDay = 7;
            applyFilterSortAndReset();
            highlightSelectedDay(tvSunday);
        });
    }

    private void clearDayHighlights() {
        TextView[] all = {
                tvMonday, tvTuesday, tvWednesday,
                tvThursday, tvFriday, tvSaturday, tvSunday
        };

        for (TextView tv : all) {
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(11f);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void highlightSelectedDay(TextView selected) {
        clearDayHighlights();

        selected.setBackgroundColor(Color.parseColor("#E0ECFF")); // light blue
        selected.setTextColor(Color.parseColor("#0A3AA6"));
        selected.setTextSize(12f);
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void applyFilterSortAndReset() {
        workingAppointments.clear();

        for (ScheduleModel m : weekAppointments) {

            boolean matchesStatus =
                    (statusFilter == 0 && m.getStatus() == 0) ||
                            (statusFilter == 1 && m.getStatus() == 1);

            boolean matchesDay =
                    (selectedDay == 0) || (m.getDayOfWeek() == selectedDay);

            if (matchesStatus && matchesDay) {
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
