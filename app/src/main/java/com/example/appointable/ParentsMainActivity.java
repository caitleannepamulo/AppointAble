package com.example.appointable;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class ParentsMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Correct fragment mapping
        fragmentMap.put(R.id.nav_home, new ParentHomeFragment());
        fragmentMap.put(R.id.nav_appointments, new AppointmentsFragment());
        fragmentMap.put(R.id.nav_messages, new MessagesFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        // Listener for bottom nav
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentMap.get(item.getItemId());
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        // IMPORTANT: Set the selected tab instead of calling loadFragment manually
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // This loads Home first
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
