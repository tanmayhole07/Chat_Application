package com.example.socialm;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialm.adapters.AdapterChat;
import com.example.socialm.models.ModelChat;
import com.example.socialm.models.ModelUsers;
import com.example.socialm.notifications.Data;
import com.example.socialm.notifications.Sender;
import com.example.socialm.notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import retrofit2.Call;
//import retrofit2.Callback;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ChatActivity extends AppCompatActivity {
    AdapterChat adapterChat;
    private RequestQueue requestQueue;

    List<ModelChat> chatList;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;

    String hisImage;
    String hisUid;
    String myUid;

    EditText messageEt;
    TextView nameTv;
    private boolean notify = false;
    ImageView profileIv, blockIv;

    RecyclerView recyclerView;
    ValueEventListener seenListener;

    ImageButton sendBtn, attachBtn;
    Toolbar toolbar;

    DatabaseReference userDbref;
    DatabaseReference userRefForSeen;
    TextView userStatusTv;

    boolean isBlocked = false;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    String[] cameraPermissions;
    String[] storagePermissions;

    Uri image_uri = null;

    private static final String SECRET_KEY = "123456789";
    private static final String SALTVALUE = "abcdefg";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        attachBtn = findViewById(R.id.attachBtn);
        blockIv = findViewById(R.id.blockIv);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        firebaseAuth = FirebaseAuth.getInstance();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        firebaseDatabase  = FirebaseDatabase.getInstance();
        userDbref = firebaseDatabase.getReference("Users");

        Query userQuery = userDbref.orderByChild("uid").equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();

                    String typingStatus = "" + ds.child("typingTo").getValue();
                    if (typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }else {
                        String onlineStatus =  "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            ChatActivity.this.userStatusTv.setText(onlineStatus);
                        }
                        else {
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                            userStatusTv.setText("Last seen at: " + dateTime);
                        }

                    }


                    nameTv.setText(name);
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img).into(profileIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_img).into(profileIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //notify = true;
                String message = messageEt.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(ChatActivity.this, "Cannot send the empty message", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(message);
                }
                messageEt.setText("");
            }
        });

        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showImagePickDialog();

            }
        });

        messageEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                    return;
                }
                checkTypingStatus(hisUid);
            }

            public void afterTextChanged(Editable editable) {
            }
        });

        blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBlocked){
                    unBlockUser();
                }else {
                    blockUser();
                }
            }
        });

        readMessages();
        checkIsBlocked();
        seenMessage();
    }

    private void checkIsBlocked() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            blockIv.setImageResource(R.drawable.ic_block_red);
                            isBlocked=true;                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void blockUser() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid",hisUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChatActivity.this, "Blocked Successfully ", Toast.LENGTH_SHORT).show();

                        blockIv.setImageResource(R.drawable.ic_block_red);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Failed :"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(ChatActivity.this, "UnBlocked Successfully", Toast.LENGTH_SHORT).show();

                                                blockIv.setImageResource(R.drawable.ic_unblocked_green);

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ChatActivity.this, "Failed :"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


    }

    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (which == 0){
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();
                    }
                }
                if (which == 1){
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), IMAGE_PICK_GALLERY_CODE);

    }

    private void pickFromCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Descr");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void readMessages() {
        chatList = new ArrayList();
        DatabaseReference dbRef =  FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat =ds.getValue(ModelChat.class);
                    if ((chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) ||
                            (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid))) {
                        chatList.add(chat);
                    }
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(ChatActivity.this.adapterChat);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /* Encryption Method */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String encrypt(String strToEncrypt)
    {
        try
        {
            /* Declare a byte array. */
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            /* Create factory for secret keys. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            /* PBEKeySpec class implements KeySpec interface. */
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALTVALUE.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            /* Retruns encrypted value. */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder()
                        .encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
            }
        }
        catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
        {
            System.out.println("Error occured during encryption: " + e.toString());
        }
        return null;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String strToDecrypt)
    {
        try
        {
            /* Declare a byte array. */
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            /* Create factory for secret keys. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            /* PBEKeySpec class implements KeySpec interface. */
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALTVALUE.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            /* Retruns decrypted value. */
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
        {
            System.out.println("Error occured during decryption: " + e.toString());
        }
        return null;
    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendMessage(final String message) {


        String encryptedMessage = encrypt(message);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", encryptedMessage);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type", "text");

        databaseReference.child("Chats").push().setValue(hashMap);
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                ModelUsers user = dataSnapshot.getValue(ModelUsers.class);
                if (notify) {
                    senNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }
            public void onCancelled(DatabaseError databaseError) {
            }
        });
       //messageEt.setText("");

        DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(myUid)
                .child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(hisUid)
                .child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void senNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name+","+message,"New Message", hisUid, R.drawable.ic_default_img);
                    Sender sender = new Sender(data, token.getToken());

                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("JSON_RESPONSE","onResponse : " + response.toString());
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE","onResponse : " + error.toString());
                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError{
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type","application/json");
                                headers.put("Authorization", "key=AAAA1CIFI1Y:APA91bEqeN0WC0f8xIFLSacHx95ULIUycYBpIFSkNtjiWMzVYv8rcS4eOerZ_T6Skx4zAikxQqrz665277uX4oSTCIHyuBlgMdWn2rOsO8uVbz9sa_egV_wTSh1qbSMUkNN2jjvy-Gd2");
                                return super.getHeaders();
                            }
                        };
                        requestQueue.add(jsonObjectRequest);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
            return;
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }

    public void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        dbRef.updateChildren(hashMap);
    }


    public void onPause() {
        super.onPause();
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }


    public void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    public void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){
                        pickFromCamera();
                    }else {
                        Toast.makeText(this, "Camera & Storage Permissions both are necessary", Toast.LENGTH_SHORT).show();
                    }
                }else {

                }
                break;

            case STORAGE_REQUEST_CODE:

                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        pickFromGallery();
                    }else {
                        Toast.makeText(this, "Storage permissions are Required", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();

                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void sendImageMessage(Uri image_uri) throws IOException   {
        notify = true;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending Message...");
        progressDialog.show();

        String timestmp = ""+ System.currentTimeMillis();
        String fileNameAndPath = "ChatImages/"+"post_"+timestmp;

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()){
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender",myUid);
                            hashMap.put("receiver",hisUid);
                            hashMap.put("message",downloadUri);
                            hashMap.put("timestamp",timestmp);
                            hashMap.put("type","image");
                            hashMap.put("isSeen",false);

                            databaseReference.child("Chats").push().setValue(hashMap);

                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                            database.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ModelUsers user = dataSnapshot.getValue(ModelUsers.class);
                                    if (notify){
                                        senNotification(hisUid, user.getName(),"Sent you a Photo...");
                                        notify=false;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                                    .child(myUid)
                                    .child(hisUid);
                            chatRef1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()){
                                        chatRef1.child("id").setValue(hisUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                                    .child(hisUid)
                                    .child(myUid);

                            chatRef2.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()){
                                        chatRef2.child("id").setValue(myUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
            }
        });



    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
