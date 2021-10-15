package com.example.videoeditor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addVideosBtn;

    private ArrayList<VideoListModel> videoArrayList;
    private RecyclerView videosRecyclerView;
    private Adapter adapterVideo;

    Button logout;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Videos");

        addVideosBtn = findViewById(R.id.uploadVideo);
        videosRecyclerView = findViewById(R.id.videosRecyclerView);
        logout = findViewById(R.id.logout);
        auth = FirebaseAuth.getInstance();

        loadVideosFromFirebase();

        addVideosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddVideoActivity.class));
            }
        });
    }

    private void loadVideosFromFirebase() {
        videoArrayList = new ArrayList<>();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                videoArrayList.clear();
                for(DataSnapshot snapshot1: snapshot.getChildren()) {
                    VideoListModel videoListModel = snapshot1.getValue(VideoListModel.class);
                    videoArrayList.add(videoListModel);
                }

                adapterVideo = new Adapter(MainActivity.this, videoArrayList);
                videosRecyclerView.setAdapter(adapterVideo);
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                auth.signOut();
                break;

        }
        return true;
    }
}