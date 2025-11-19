package com.example.appointable;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class ParentsMainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        fragmentMap.put(R.id.nav_parent_home, new ParentHomeFragment());
        fragmentMap.put(R.id.nav_appointments, new AppointmentsFragment());
        fragmentMap.put(R.id.nav_students, new StudentsFragment());
        fragmentMap.put(R.id.nav_messages, new MessagesFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        loadFragment(new ParentHomeFragment());

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
