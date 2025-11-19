package com.example.appointable;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class TeachersMainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make sure this layout has: bottom_navigation + fragment_container
        setContentView(R.layout.activity_parent_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // map each nav item to the correct TEACHER fragment
        fragmentMap.put(R.id.nav_home, new TeacherHomeFragment());
        fragmentMap.put(R.id.nav_appointments, new Schedule_TeacherFragment());
        fragmentMap.put(R.id.nav_students, new Student_TeacherFragment());
        fragmentMap.put(R.id.nav_messages, new Messages_TeacherFragment());
        fragmentMap.put(R.id.nav_profile, new Profile_TeacherFragment());

        loadFragment(new TeacherHomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentMap.get(item.getItemId());
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
