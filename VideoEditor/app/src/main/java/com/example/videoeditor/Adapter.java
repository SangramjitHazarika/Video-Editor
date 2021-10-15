package com.example.videoeditor;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class Adapter extends RecyclerView.Adapter<Adapter.VideoHolder>{
    
    private Context context;
    private ArrayList<VideoListModel> videoArrayList;

    public Adapter(Context context, ArrayList<VideoListModel> videoArrayList) {
        this.context = context;
        this.videoArrayList = videoArrayList;
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_list, parent, false);
        return new VideoHolder(view);
    }

    @Override
    public void onBindViewHolder(Adapter.VideoHolder holder, int position) {
        VideoListModel videoListModel = videoArrayList.get(position);

        String id = videoListModel.getId();
        String title = videoListModel.getTitle();
        String tags = videoListModel.getTags();
        String timestamp = videoListModel.getTimestamp();
        String videoUrl = videoListModel.getVideoUrl();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        DateFormat isoFormat = new SimpleDateFormat("dd/MM/yyyy K:mm a");
        String formattedDate = isoFormat.format(calendar.getTime());

        holder.titleVideo.setText(title);
        holder.tagsVideo.setText(tags);
        holder.timeVideo.setText(formattedDate);
        setVideoUrl(videoListModel, holder);

        holder.downloadFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadVideo(videoListModel);
            }
        });

        holder.deleteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete")
                        .setMessage("Are you sure you want to delete the video: " + title)
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteVideo(videoListModel);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        holder.speedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setPlaybackParams(mp.getPlaybackParams().setSpeed((mp.getPlaybackParams().getSpeed())*2.5f));
                        mp.start();
                        holder.videoView.start();
                    }
                });
            }
        });
    }

    private void setVideoUrl(VideoListModel videoListModel, VideoHolder holder) {
        holder.progressBar.setVisibility(View.VISIBLE);

        String videoUrl = videoListModel.getVideoUrl();

        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri = Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                holder.progressBar.setVisibility(View.GONE);
                mp.start();
            }
        });



        holder.videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch(what) {
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:{
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START: {
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END: {
                        holder.progressBar.setVisibility(View.GONE);
                        return true;
                    }

                }
                return false;
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
    }

    private void deleteVideo(VideoListModel videoListModel) {
        String videoId = videoListModel.getId();
        String videoUrl = videoListModel.getVideoUrl();

        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        reference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
                        databaseReference.child(videoId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(context, "Video Deleted!!!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void downloadVideo(VideoListModel videoListModel) {
        final String videoUrl = videoListModel.getVideoUrl();
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        storageReference.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        String fileName = storageMetadata.getName();
                        String fileType = storageMetadata.getContentType();
                        String fileDirectory = Environment.DIRECTORY_DOWNLOADS;

                        DownloadManager downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

                        Uri uri = Uri.parse(videoUrl);

                        DownloadManager.Request request = new DownloadManager.Request(uri);

                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir("" + fileDirectory, "" + fileName + ".mp4");

                        downloadManager.enqueue(request);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return videoArrayList.size();
    }

    class VideoHolder extends RecyclerView.ViewHolder{

        VideoView videoView;
        TextView titleVideo;
        TextView timeVideo;
        TextView tagsVideo;
        ProgressBar progressBar;
        FloatingActionButton speedFab;
        FloatingActionButton deleteFab;
        FloatingActionButton downloadFab;

        public VideoHolder(View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoViewlist);
            titleVideo = itemView.findViewById(R.id.titleVideo);
            timeVideo = itemView.findViewById(R.id.timeVideo);
            tagsVideo = itemView.findViewById(R.id.tagsVideo);
            progressBar = itemView.findViewById(R.id.progressBar);
            speedFab = itemView.findViewById(R.id.speedFab);
            deleteFab = itemView.findViewById(R.id.deleteFab);
            downloadFab = itemView.findViewById(R.id.downloadFab);

        }
    }
}
