package com.example.socialm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.socialm.fragments.ChatListFragment;
import com.example.socialm.fragments.HomeFragment;
import com.example.socialm.fragments.NotificationFragment;
import com.example.socialm.fragments.ProfileFragment;
import com.example.socialm.fragments.UsersFragment;
import com.example.socialm.notifications.Token;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class DashboardActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    String mUID = "uid";
    ActionBar actionBar;

    private BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        actionBar = getSupportActionBar();
        firebaseAuth = FirebaseAuth.getInstance();

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        actionBar.setTitle("Users");
        UsersFragment fragment3 = new UsersFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment3, "");
        ft1.commit();

        checkUserStatus();


    }

    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    switch (menuItem.getItemId()) {

                        case R.id.nav_users:
                            actionBar.setTitle("Users");
                            UsersFragment fragment3 = new UsersFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content, fragment3, "");
                            ft3.commit();
                            return true;

                        case R.id.nav_chat:
                            actionBar.setTitle("Chats");
                            ChatListFragment fragment4 = new ChatListFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content, fragment4, "");
                            ft4.commit();
                            return true;


                    }
                    return false;
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showMoreOptions() {

        PopupMenu popupMenu = new PopupMenu(this, navigationView, Gravity.END);

        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Notifications");
        popupMenu.getMenu().add(Menu.NONE, 1, 0, "Group Chats");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id==0){
                    actionBar.setTitle("Notifications");
                    NotificationFragment fragment5 = new NotificationFragment();
                    FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                    ft5.replace(R.id.content, fragment5, "");
                    ft5.commit();

                }else if (id==1){
                    actionBar.setTitle("Group Chat");
                    GroupChatFragment fragment6 = new GroupChatFragment();
                    FragmentTransaction ft6 = getSupportFragmentManager().beginTransaction();
                    ft6.replace(R.id.content, fragment6, "");
                    ft6.commit();

                }
                return false;
            }
        });
        popupMenu.show();
    }


    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //mProfileTv.setText(user.getEmail());
            mUID = user.getUid();

            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();

            updateToken(FirebaseInstanceId.getInstance().getToken());

        } else {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }


}