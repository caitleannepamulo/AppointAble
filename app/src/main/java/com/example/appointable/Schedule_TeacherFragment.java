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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Schedule_TeacherFragment extends Fragment {

    private static final int STATUS_ACCEPTED = 0;
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_CANCELED = 2;

    private RecyclerView rvAllAppointments;
    private TextView tvRemainingCount, tvCompletedCount, tvCanceledCount;
    private ImageView ivSort;
    private CardView cardRemainingAppointments, cardCompletedAppointments, cardCanceledAppointments;
    private TextView tvMonday, tvTuesday, tvWednesday, tvThursday, tvFriday, tvSaturday, tvSunday;

    private final List<ScheduleModel> weekAppointments = new ArrayList<>();
    private final List<ScheduleModel> workingAppointments = new ArrayList<>();
    private final List<ScheduleModel> displayedAppointments = new ArrayList<>();
    private ScheduleAdapter adapter;
    private int currentIndex = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isAscending = true;

    // filters
    private int statusFilter = STATUS_ACCEPTED; // 0 = accepted, 1 = completed, 2 = canceled
    private int selectedDay = 0;               // 0 = none, 1..7 = Mon..Sun
    private int weekOffset = 0;

    // current visible week (for filtering)
    private int currentWeekYear;
    private int currentWeekOfYear;

    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public Schedule_TeacherFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule_teacher, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvRemainingCount = view.findViewById(R.id.tvRemainingCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvCanceledCount = view.findViewById(R.id.tvCanceledCount);

        rvAllAppointments = view.findViewById(R.id.rvAllAppointments);
        ivSort = view.findViewById(R.id.ivSort);

        cardRemainingAppointments = view.findViewById(R.id.cardRemainingAppointments);
        cardCompletedAppointments = view.findViewById(R.id.cardCompletedAppointments);
        cardCanceledAppointments = view.findViewById(R.id.cardCanceledAppointments);

        tvMonday = view.findViewById(R.id.tvMonday);
        tvTuesday = view.findViewById(R.id.tvTuesday);
        tvWednesday = view.findViewById(R.id.tvWednesday);
        tvThursday = view.findViewById(R.id.tvThursday);
        tvFriday = view.findViewById(R.id.tvFriday);
        tvSaturday = view.findViewById(R.id.tvSaturday);
        tvSunday = view.findViewById(R.id.tvSunday);

        setupRecyclerView();
        setupSortButton();
        setupFilterCards();
        setupDayClickListeners();
        setupSwipeGesture(view);

        setWeekDayLabels();
        autoSelectToday(); // will highlight and filter for current week only

        loadAppointmentsFromFirestore();

        return view;
    }

    // ------------------ Firestore load ------------------

    private void loadAppointmentsFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = user.getUid();

        db.collection("appointments")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnCompleteListener(this::onAppointmentsLoaded);
    }

    private void onAppointmentsLoaded(@NonNull Task<QuerySnapshot> task) {
        if (!task.isSuccessful()) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "Failed to load appointments.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        weekAppointments.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        for (QueryDocumentSnapshot doc : task.getResult()) {
            String childName = doc.getString("childName");
            String service = doc.getString("service");
            String timeStr = doc.getString("time");
            String dateStr = doc.getString("date");
            String statusStr = doc.getString("status");
            String docId = doc.getId();

            // ---- STATUS MAPPING FIX ----
            if (statusStr == null) {
                // no status → skip
                continue;
            }

            int status;
            if (statusStr.equalsIgnoreCase("Completed")) {
                status = STATUS_COMPLETED;
            } else if (statusStr.equalsIgnoreCase("Canceled")
                    || statusStr.equalsIgnoreCase("Cancelled")) {
                status = STATUS_CANCELED;
            } else if (statusStr.equalsIgnoreCase("Accepted")
                    || statusStr.equalsIgnoreCase("Rescheduled")) {
                // Rescheduled is counted as remaining/accepted
                status = STATUS_ACCEPTED;
            } else {
                // anything else like "Pending", "Pending Fetching", etc. → skip
                continue;
            }
            // ----------------------------

            int sortMinutes = parseTimeToMinutes(timeStr);
            int dayOfWeek = 0;
            int weekYear = 0;
            int weekOfYear = 0;

            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    Date date = dateFormat.parse(dateStr);
                    if (date != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        dayOfWeek = mapCalendarDayToCustom(cal.get(Calendar.DAY_OF_WEEK));
                        weekYear = cal.get(Calendar.YEAR);
                        weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            ScheduleModel m = new ScheduleModel(
                    childName != null ? childName : "",
                    service != null ? service : "",
                    timeStr != null ? timeStr : "",
                    sortMinutes,
                    dayOfWeek,
                    docId,
                    dateStr != null ? dateStr : "",
                    weekYear,
                    weekOfYear
            );
            m.setStatus(status);

            weekAppointments.add(m);
        }

        setStatsFromData();
        setWeekDayLabels();
        autoSelectToday();  // re-apply now that data exists
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null) return 0;
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = timeFormat.parse(timeStr);
            if (date == null) return 0;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            return hour * 60 + minute;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ------------------ swipe / week navigation ------------------

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

        // reset day selection
        selectedDay = 0;
        clearDayHighlights();

        setWeekDayLabels(); // this also updates currentWeekYear/currentWeekOfYear

        if (weekOffset == 0) {
            // current week → auto-select today again
            autoSelectToday();
        } else {
            // other weeks → do NOT show any appointments until user taps a day
            displayedAppointments.clear();
            adapter.notifyDataSetChanged();
        }
    }

    // ------------------ weekday labels / current week ------------------

    private void setWeekDayLabels() {
        Calendar realToday = Calendar.getInstance();

        // today mapped to our Mon=1..Sun=7
        int todayMapped = mapCalendarDayToCustom(realToday.get(Calendar.DAY_OF_WEEK));

        // Monday of "current" visible week
        Calendar monday = (Calendar) realToday.clone();
        monday.add(Calendar.DAY_OF_MONTH, 1 - todayMapped);
        monday.add(Calendar.WEEK_OF_YEAR, weekOffset);

        // store current visible week for filtering
        currentWeekYear = monday.get(Calendar.YEAR);
        currentWeekOfYear = monday.get(Calendar.WEEK_OF_YEAR);

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

        setDayLabel(tvMonday, "Mon", monday, realToday, dateFormat);
        setDayLabel(tvTuesday, "Tue", tuesday, realToday, dateFormat);
        setDayLabel(tvWednesday, "Wed", wednesday, realToday, dateFormat);
        setDayLabel(tvThursday, "Thu", thursday, realToday, dateFormat);
        setDayLabel(tvFriday, "Fri", friday, realToday, dateFormat);
        setDayLabel(tvSaturday, "Sat", saturday, realToday, dateFormat);
        setDayLabel(tvSunday, "Sun", sunday, realToday, dateFormat);
    }

    private void setDayLabel(TextView tv,
                             String dayShortName,
                             Calendar dayCal,
                             Calendar realToday,
                             SimpleDateFormat dateFormat) {

        String dateText;
        if (isSameDay(dayCal, realToday) && weekOffset == 0) {
            dateText = "Today";
        } else {
            dateText = dateFormat.format(dayCal.getTime());
        }

        tv.setText(dayShortName + "\n" + dateText);
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

        statusFilter = STATUS_ACCEPTED;

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

    // ------------------ stats ------------------

    private void setStatsFromData() {
        int accepted = 0;
        int completed = 0;
        int canceled = 0;

        for (ScheduleModel m : weekAppointments) {
            if (m.getStatus() == STATUS_COMPLETED) {
                completed++;
            } else if (m.getStatus() == STATUS_CANCELED) {
                canceled++;
            } else if (m.getStatus() == STATUS_ACCEPTED) {
                accepted++;
            }
        }

        tvRemainingCount.setText(String.valueOf(accepted));
        tvCompletedCount.setText(String.valueOf(completed));
        tvCanceledCount.setText(String.valueOf(canceled));
    }

    // ------------------ recycler / filters ------------------

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvAllAppointments.setLayoutManager(layoutManager);

        adapter = new ScheduleAdapter(
                displayedAppointments,
                () -> {
                    setStatsFromData();
                    applyFilterSortAndReset();
                }
        );

        rvAllAppointments.setAdapter(adapter);
    }

    private void setupSortButton() {
        ivSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            applyFilterSortAndReset();
        });
    }

    private void setupFilterCards() {
        cardRemainingAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_ACCEPTED;
            selectedDay = 0;
            clearDayHighlights();
            // no day selected → clear list until user taps a day
            displayedAppointments.clear();
            adapter.notifyDataSetChanged();
        });

        cardCompletedAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_COMPLETED;
            selectedDay = 0;
            clearDayHighlights();
            displayedAppointments.clear();
            adapter.notifyDataSetChanged();
        });

        cardCanceledAppointments.setOnClickListener(v -> {
            statusFilter = STATUS_CANCELED;
            selectedDay = 0;
            clearDayHighlights();
            displayedAppointments.clear();
            adapter.notifyDataSetChanged();
        });
    }

    private void setupDayClickListeners() {
        tvMonday.setOnClickListener(v -> {
            selectedDay = 1;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvMonday);
        });

        tvTuesday.setOnClickListener(v -> {
            selectedDay = 2;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvTuesday);
        });

        tvWednesday.setOnClickListener(v -> {
            selectedDay = 3;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvWednesday);
        });

        tvThursday.setOnClickListener(v -> {
            selectedDay = 4;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvThursday);
        });

        tvFriday.setOnClickListener(v -> {
            selectedDay = 5;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvFriday);
        });

        tvSaturday.setOnClickListener(v -> {
            selectedDay = 6;
            statusFilter = STATUS_ACCEPTED;
            applyFilterSortAndReset();
            highlightSelectedDay(tvSaturday);
        });

        tvSunday.setOnClickListener(v -> {
            selectedDay = 7;
            statusFilter = STATUS_ACCEPTED;
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
            tv.setBackground(null);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(11f);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void highlightSelectedDay(TextView selected) {
        clearDayHighlights();
        selected.setBackgroundResource(R.drawable.bg_day_select);
        selected.setTextColor(Color.WHITE);
        selected.setTextSize(13f);
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void applyFilterSortAndReset() {
        workingAppointments.clear();

        for (ScheduleModel m : weekAppointments) {

            boolean matchesStatus = false;
            switch (statusFilter) {
                case STATUS_ACCEPTED:
                    matchesStatus = (m.getStatus() == STATUS_ACCEPTED);
                    break;
                case STATUS_COMPLETED:
                    matchesStatus = (m.getStatus() == STATUS_COMPLETED);
                    break;
                case STATUS_CANCELED:
                    matchesStatus = (m.getStatus() == STATUS_CANCELED);
                    break;
            }

            // match week
            boolean matchesWeek =
                    m.getWeekYear() == currentWeekYear &&
                            m.getWeekOfYear() == currentWeekOfYear;

            // match day (only if user selected one)
            boolean matchesDay =
                    (selectedDay == 0) || (m.getDayOfWeek() == selectedDay);

            if (matchesStatus && matchesWeek && matchesDay) {
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
        displayedAppointments.addAll(workingAppointments);

        adapter.notifyDataSetChanged();
        currentIndex = workingAppointments.size();
        isLoading = false;
    }

    private void loadNextPage() {
        // Ready if you want real pagination later
        if (currentIndex >= workingAppointments.size()) return;
    }
}
