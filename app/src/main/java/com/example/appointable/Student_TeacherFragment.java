package com.example.appointable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Student_TeacherFragment extends Fragment {

    private RecyclerView rvStudents;
    private TextView tvRemainingCount;
    private List<StudentModel> students;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_student_teacher, container, false);

        rvStudents = view.findViewById(R.id.rvStudents);
        tvRemainingCount = view.findViewById(R.id.tvRemainingCount);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudents.setHasFixedSize(true);

        loadDummyData();

        StudentAdapter adapter = new StudentAdapter(students);
        rvStudents.setAdapter(adapter);

        tvRemainingCount.setText(String.valueOf(students.size()));

        return view;
    }

    private void loadDummyData() {
        students = new ArrayList<>();

        students.add(new StudentModel("2025001", "Anna", "Cruz", "", "", "89", "Active"));
        students.add(new StudentModel("2025002", "Mark", "Reyes", "", "", "92", "Active"));
        students.add(new StudentModel("2025003", "Liza", "Gomez", "", "", "75", "Irregular"));
        students.add(new StudentModel("2025004", "John", "Santos", "", "", "81", "Active"));
        students.add(new StudentModel("2025005", "Ella", "Ramos", "", "", "95", "Active"));
        students.add(new StudentModel("2025006", "Paolo", "Dizon", "", "", "70", "Dropped"));
    }
}
