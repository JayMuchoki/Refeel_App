package com.first.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.first.chatapp.Utills.Comment;
import com.first.chatapp.Utills.Posts;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyPost_Activity extends AppCompatActivity  {
    Toolbar toolbar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mUserRef,PostRef,likeRef,CommentRef;
    String profileImageUrlV,usernameV;
    CircleImageView profileImageHeader;
    TextView usernameHeader;
    ImageView addimagePost,sendImagepost;
    EditText inputPostDesc;
    private static final int REQUEST_CODE = 101;
    Uri imageuri;
    ProgressDialog mLoadingBar;
    StorageReference postImageRef;
    FirebaseRecyclerAdapter<Posts,MyViewHolder>adapter;
    FirebaseRecyclerOptions<Posts>options;
    RecyclerView recyclerView;
    FirebaseRecyclerOptions<Comment>CommentOtion;
    FirebaseRecyclerAdapter<Comment,CommentViewHolder>CommentAdapter;








    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_post);

        toolbar=findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Posts ");
       

        addimagePost=findViewById(R.id.addimagepost);
        sendImagepost=findViewById(R.id.send_post_imageview);
        inputPostDesc=findViewById(R.id.inputpostDesc);

        mLoadingBar=new ProgressDialog(this);
        recyclerView=findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAuth=FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mUserRef= FirebaseDatabase.getInstance().getReference().child("Users");
        PostRef= FirebaseDatabase.getInstance().getReference().child("Posts");
        likeRef= FirebaseDatabase.getInstance().getReference().child("Likes");
        CommentRef= FirebaseDatabase.getInstance().getReference().child("Comments");
        postImageRef= FirebaseStorage.getInstance().getReference().child("PostImages");

        FirebaseMessaging.getInstance().subscribeToTopic(mUser.getUid());

        drawerLayout=findViewById(R.id.drawerlayout);
        navigationView=findViewById(R.id.navView);

        View view=navigationView.inflateHeaderView(R.layout.drawer_header);
        profileImageHeader=view.findViewById(R.id.profileImage_header);
        usernameHeader=view.findViewById(R.id.username_header);




        sendImagepost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddPost();
            }
        });
        addimagePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,REQUEST_CODE);
            }
        });
        LoadPost();

        getFCMToken();


    }

    void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                String token=task.getResult();

            }
        });
    }

    private void LoadPost() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        Query query = PostRef.orderByChild("uid").equalTo(mUser.getUid());

        options = new FirebaseRecyclerOptions.Builder<Posts>().setQuery(query, Posts.class).build();
        adapter = new FirebaseRecyclerAdapter<Posts, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Posts model) {
                String postKey = getRef(position).getKey();
                holder.postDesc.setText(model.getPostDesc());
                String timeAgo = calculateTimeAgo(model.getDatePost());
                holder.timeago.setText(timeAgo);
                holder.username.setText(model.getUsername());

                // Check if the postimageurl is not empty before loading the image
                if (!model.getPostimageurl().isEmpty()) {
                    holder.postImage.setVisibility(View.VISIBLE); // Set visibility to VISIBLE
                    Picasso.get().load(model.getPostimageurl()).into(holder.postImage);
                } else {
                    // Set visibility to GONE if no image is available
                    holder.postImage.setVisibility(View.GONE);
                }

                Picasso.get().load(model.getUserProfileImageUrl()).into(holder.profileImage);
                holder.countLikes(postKey, mUser.getUid(), likeRef);
                holder.countComments(postKey, mUser.getUid(), CommentRef);
                holder.commentsImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MyPost_Activity.this, PostCommentsActivity.class);
                        intent.putExtra("postKey", postKey); // Pass the post key to the PostCommentsActivity
                        startActivity(intent);
                    }
                });
                holder.likeImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        likeRef.child(postKey).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    likeRef.child(postKey).child(mUser.getUid()).removeValue();
                                    holder.likeImage.setColorFilter(Color.GRAY);
                                    notifyDataSetChanged();
                                } else {
                                    likeRef.child(postKey).child(mUser.getUid()).setValue("like");
                                    holder.likeImage.setColorFilter(Color.GREEN);
                                    notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MyPost_Activity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                holder.commentSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String comment = holder.inputComments.getText().toString();
                        if (comment.isEmpty()) {
                            Toast.makeText(MyPost_Activity.this, "Please write something in EditText", Toast.LENGTH_SHORT).show();
                        } else {
                            AddComment(holder, postKey, CommentRef, mUser.getUid(), comment);
                        }
                    }
                });
                holder.postImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(MyPost_Activity.this,ImageViewActivity.class);
                        intent.putExtra("url",model.getPostimageurl());
                        startActivity(intent);
                    }
                });
                holder.profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(MyPost_Activity.this,FindFriendActivity.class);
                        intent.putExtra("OtherUserId",getRef(position).getKey().toString());
                        startActivity(intent);
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        // Show a confirmation dialog before deleting the post
                        AlertDialog.Builder builder = new AlertDialog.Builder(MyPost_Activity.this);
                        builder.setTitle("Delete Post");
                        builder.setMessage("Are you sure you want to delete this post?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Delete the post from the database
                                PostRef.child(postKey).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(MyPost_Activity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MyPost_Activity.this, "Failed to delete post", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Dismiss the dialog if the user clicks "No"
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();

                        return true;
                    }
                });


            }


            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_view_post, parent, false);
                return new MyViewHolder(view);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }


    private void LoadComments(MyViewHolder holder, String postKey) {
        RecyclerView commentRecyclerView = holder.itemView.findViewById(R.id.recyclerViewComments); // Assuming you have a RecyclerView in your single_view_post layout to display comments
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(MyPost_Activity.this));

        FirebaseRecyclerOptions<Comment> commentOption = new FirebaseRecyclerOptions.Builder<Comment>().setQuery(CommentRef.child(postKey), Comment.class).build();
        FirebaseRecyclerAdapter<Comment, CommentViewHolder> commentAdapter = new FirebaseRecyclerAdapter<Comment, CommentViewHolder>(commentOption) {
            @Override
            protected void onBindViewHolder(@NonNull CommentViewHolder holder, int position, @NonNull Comment model) {
                Picasso.get()
                        .load(model.getProfileImageUrl())
                        .resize(100, 100)  // Resize the image to 100x100 pixels
                        .centerCrop()      // Crop the image to fit the ImageView
                        .into(holder.profileImage);

                holder.username.setText(model.getUsername());
                holder.comment.setText(model.getComment());


            }

            @NonNull
            @Override
            public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_view_comment, parent, false);
                return new CommentViewHolder(view);
            }
        };
        commentAdapter.startListening();
        commentRecyclerView.setAdapter(commentAdapter);
    }


    private void AddComment(MyViewHolder holder, String postKey, DatabaseReference commentRef, String uid, String comment) {

        // Create a new DatabaseReference for comments under the specific postKey
        DatabaseReference postCommentRef = commentRef.child(postKey).push();

        // Create a HashMap to store comment details
        HashMap<String, Object> commentMap = new HashMap<>();
        commentMap.put("username", usernameV);
        commentMap.put("profileImageUrl", profileImageUrlV);
        commentMap.put("comment", comment);

        // Set the comment details under a new child node with a unique key
        postCommentRef.setValue(commentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MyPost_Activity.this, "Comment added", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    holder.inputComments.setText(null);
                } else {
                    Toast.makeText(MyPost_Activity.this, "Failed to add comment: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String calculateTimeAgo(String datePost) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        try {
            long time = sdf.parse(datePost).getTime();
            long now = System.currentTimeMillis();
            CharSequence ago =
                    DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
            return ago.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE && resultCode==RESULT_OK && data!=null){
            imageuri=data.getData();
            addimagePost.setImageURI(imageuri);
        }
    }

    private void AddPost() {
        String postDesc = inputPostDesc.getText().toString();
        if (postDesc.isEmpty() || postDesc.length() < 3) {
            inputPostDesc.setError("Please write something in the post description");
        } else {
            mLoadingBar.setTitle("Adding Post");
            mLoadingBar.setCanceledOnTouchOutside(false);
            mLoadingBar.show();

            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String strDate = formatter.format(date);

            // Check if an image is selected
            if (imageuri != null) {
                postImageRef.child(mUser.getUid() + strDate).putFile(imageuri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            postImageRef.child(mUser.getUid() + strDate).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Image uploaded successfully, add post with image URL
                                    HashMap hashMap = new HashMap();
                                    hashMap.put("datePost", strDate);
                                    hashMap.put("postimageurl", uri.toString());
                                    hashMap.put("postDesc", postDesc);
                                    hashMap.put("userProfileImageUrl", profileImageUrlV);
                                    hashMap.put("username", usernameV);
                                    hashMap.put("uid",mUser.getUid());

                                    // Use push() to generate a unique key for each post
                                    PostRef.push().setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                mLoadingBar.dismiss();
                                                Toast.makeText(MyPost_Activity.this, "Post Added", Toast.LENGTH_SHORT).show();
                                                addimagePost.setImageResource(R.drawable.ic_add_post_image);
                                                inputPostDesc.setText("");
                                            } else {
                                                mLoadingBar.dismiss();
                                                Toast.makeText(MyPost_Activity.this, "" + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                                }
                            });
                        } else {
                            mLoadingBar.dismiss();
                            Toast.makeText(MyPost_Activity.this, "" + task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            } else {
                // Posting without an image
                HashMap hashMap = new HashMap();
                hashMap.put("datePost", strDate);
                hashMap.put("postimageurl", ""); // Empty string for no image
                hashMap.put("postDesc", postDesc);
                hashMap.put("userProfileImageUrl", profileImageUrlV);
                hashMap.put("username", usernameV);

                // Use push() to generate a unique key for each post
                PostRef.push().setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mLoadingBar.dismiss();
                            Toast.makeText(MyPost_Activity.this, "Post Added", Toast.LENGTH_SHORT).show();
                            addimagePost.setImageResource(R.drawable.ic_add_post_image);
                            inputPostDesc.setText("");
                        } else {
                            mLoadingBar.dismiss();
                            Toast.makeText(MyPost_Activity.this, "" + task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        if (mUser==null){
            SendUserToLoginActivity();
        }else {
            mUserRef.child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        profileImageUrlV=dataSnapshot.child("profileImage").getValue().toString();
                        usernameV=dataSnapshot.child("username").getValue().toString();
                        Picasso.get().load(profileImageUrlV).into(profileImageHeader);
                        usernameHeader.setText(usernameV);


                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MyPost_Activity.this, "Sorry Something Went Wrong", Toast.LENGTH_SHORT).show();

                }
            });
        }

    }

    private void SendUserToLoginActivity() {
        Intent intent=new Intent(MyPost_Activity.this,LoginActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            drawerLayout.openDrawer((GravityCompat.START));
            return true;
        }
        return true;
    }
}