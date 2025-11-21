package com.example.appointable;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeachersMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                selectedFragment = new TeacherHomeFragment();
            } else if (id == R.id.nav_appointments) {
                selectedFragment = new Schedule_TeacherFragment();
            } else if (id == R.id.nav_students) {
                selectedFragment = new Student_TeacherFragment();
            } else if (id == R.id.nav_messages) {
                selectedFragment = new Messages_TeacherFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new Profile_TeacherFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    public void hideBottomNav() {
        if (bottomNavigationView != null) {
            bottomNavigationView.animate()
                    .translationY(bottomNavigationView.getHeight())
                    .setDuration(200);
        }
    }

    public void showBottomNav() {
        if (bottomNavigationView != null) {
            bottomNavigationView.animate()
                    .translationY(0)
                    .setDuration(200);
        }
    }

    public void updateMessagesBadge(int count) {
        if (bottomNavigationView == null) return;

        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_messages);

        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }
}
