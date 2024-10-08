package com.first.chatapp;

import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyViewHolder extends RecyclerView.ViewHolder {

    CircleImageView profileImage;
    ImageView postImage,likeImage,commentsImage,commentSend;
    TextView username,timeago,postDesc,commentsCounter;
    TextView likecounter;
    EditText inputComments;
    public static RecyclerView recyclerView;
    public MyViewHolder(@NonNull View itemView) {
        super(itemView);

        profileImage=itemView.findViewById(R.id.profileImagePost);
        postImage=itemView.findViewById(R.id.postimage);
        username=itemView.findViewById(R.id.profileUsenamePost);
        timeago=itemView.findViewById(R.id.timeAgo);
        postDesc=itemView.findViewById(R.id.postDesc);
        likeImage=itemView.findViewById(R.id.likeImage);
        commentsImage=itemView.findViewById(R.id.commentsImage);
        likecounter=itemView.findViewById(R.id.likeCounter);
        inputComments=itemView.findViewById(R.id.inputComments);
        recyclerView=itemView.findViewById(R.id.recyclerViewComments);
        commentSend=itemView.findViewById(R.id.sendComment);


        commentsCounter=itemView.findViewById(R.id.commentsCounter);
    }

    public void countLikes(String postKey, String uid, DatabaseReference likeRef) {
        likeRef.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    int totalLikes= (int) snapshot.getChildrenCount();
                    likecounter.setText(totalLikes+"");
                }else {
                    likecounter.setText("0");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        likeRef.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(uid).exists()){
                    likeImage.setColorFilter(Color.GREEN);
                }else {
                    likeImage.setColorFilter(Color.GRAY);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void countComments(String postKey, String uid, DatabaseReference commentRef) {
        commentRef.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    int totalComents= (int) snapshot.getChildrenCount();
                    commentsCounter.setText(totalComents+"");
                }else {
                    commentsCounter.setText("0");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}
