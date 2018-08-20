package com.hackwithinfy.one.hoarding;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataSource;

public class MainActivity extends AppCompatActivity {

    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    private Button click;
    private ArrayList<String> nameLegalBoard;
    private ArrayList<String> imageLegalBoard;

    private static final int PERMS_REQUEST_CODE = 1000;
    private static final int CAMERA = 345;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChildPhotosStorageReference;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location mlocation;

    private RecyclerView recyclerView;
    private TextView text;

    private final String baseUrl ="http://192.168.43.221:5000/";

    private Uri uri;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        click = (Button)findViewById(R.id.buttonclick);
        text = (TextView)findViewById(R.id.text);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        nameLegalBoard = new ArrayList<>();
        imageLegalBoard = new ArrayList<>();

        checkPermissionsAndLocation();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference("Boards");
        //DatabaseReference reference = mFirebaseDatabase.getReference("geofire");

        recyclerView = findViewById(R.id.listContainer);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));

    }

    private void findLegalBoardId() {

        mMessagesDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                nameLegalBoard.clear();
                imageLegalBoard.clear();
                Iterable<DataSnapshot> langChildren = dataSnapshot.getChildren();
                for (DataSnapshot lang : langChildren) {
                    int k = 0;
                    Iterable<DataSnapshot> langChildren1 = lang.getChildren();
                    for (DataSnapshot lang1 : langChildren1) {

                        if (String.valueOf(lang1.getKey()).equals("location")) {
//                            Toast.makeText(getApplicationContext(), String.valueOf(lang1.getValue()), Toast.LENGTH_SHORT).show();
                            String details[] = String.valueOf(lang1.getValue()).split("&");
                            double lati = Double.parseDouble(details[0]);
                            double longi = Double.parseDouble(details[1]);
                            if (findDiff(mlocation.getLatitude(),mlocation.getLongitude(),lati,longi) <= 2000 )
                                k = 1;
                        }
                        if (String.valueOf(lang1.getKey()).equals("name") && k == 1) {
                            nameLegalBoard.add(String.valueOf(lang1.getValue()));
                        }
                        if (String.valueOf(lang1.getKey()).equals("zimage") && k == 1) {
//                            Toast.makeText(getApplicationContext(),String.valueOf(lang1.getValue()),Toast.LENGTH_SHORT).show();
                            imageLegalBoard.add(String.valueOf(lang1.getValue()));
                        }
                    }
                }
                recyclerView.setAdapter(new RecyclerViewAdapter(nameLegalBoard,imageLegalBoard,getApplicationContext()));
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }

        });
   }


    double findDiff(double lat1,double long1,double lat2,double long2){
        Location startPoint=new Location("locationA");
        startPoint.setLatitude(lat1);
        startPoint.setLongitude(long1);

        Location endPoint=new Location("locationA");
        endPoint.setLatitude(lat2);
        endPoint.setLongitude(long2);

        double distance=startPoint.distanceTo(endPoint);
        return distance;
    }

    void checkPermissionsAndLocation(){
        if(!(checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMS_REQUEST_CODE);
        } else{
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            mlocation = location;
                            //Toast.makeText(MainActivity.this,mlocation.getLatitude()+ "  "+mlocation.getLongitude(),Toast.LENGTH_LONG).show();
                            findLegalBoardId();
                            //Log.v("List",legalBoards.toString());
                        }else
                            Toast.makeText(MainActivity.this,"Not able to retrive location",Toast.LENGTH_LONG).show();
                    }
                });
            }
    }

    public void clickImage(View v){
            clickImageFromCamera();
    }

    private void clickImageFromCamera(){
        Intent CamIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File file = null;
        String imageFileName = "IMG_" + String.valueOf(System.currentTimeMillis()) + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            file = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        }catch (IOException e){}

        if(file!=null) {
            Uri photoURI = FileProvider.getUriForFile(this, "com.hackwithinfy.one.hoarding.provider", file);
            getSharedPreferences("Temp", MODE_PRIVATE).edit().putString("Uri", photoURI.toString()).apply();
            CamIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(CamIntent, CAMERA);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED  && grantResults[3] == PackageManager.PERMISSION_GRANTED && grantResults[4] == PackageManager.PERMISSION_GRANTED && grantResults[5]==PackageManager.PERMISSION_GRANTED) {
                    Log.v("PermissionsGranted", Integer.toString(grantResults.length));
                    // permission granted
                    clickImageFromCamera();

                } else { }
                Toast.makeText(this,"Permissions are necessary to run the app",Toast.LENGTH_LONG).show();
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {

                case CAMERA:
                    uri = Uri.parse(getSharedPreferences("Temp", MODE_PRIVATE).getString("Uri", ""));
                  mChildPhotosStorageReference = mFirebaseStorage.getReference();
                    final StorageReference mStorage = mChildPhotosStorageReference.child(uri.getLastPathSegment());
                    UploadTask uploadTask = mStorage.putFile(uri);
                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return mStorage.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                Toast.makeText(MainActivity.this,"Checking Validity",Toast.LENGTH_SHORT).show();
//                                mMessagesDatabaseReference = mFirebaseDatabase.getReference("photo");
//                                mMessagesDatabaseReference.child("hey1").setValue(downloadUri.toString()+"yo");
                                String url = createStringUrl(downloadUri.toString());
                                AsyncTaskRunner runner = new AsyncTaskRunner();
                                runner.execute(url);
                            } else {
                                Toast.makeText(MainActivity.this,"Not Able to Store Photos",Toast.LENGTH_LONG).show();
                            }
                        }
                    });
            }
        }
    }

    private String createStringUrl(String userClickedPic){
        Uri uri = Uri.parse(baseUrl);
        String str = uri.toString()+"?url="+userClickedPic;
        for(int i=0;i<imageLegalBoard.size();i++)
            str += " "+imageLegalBoard.get(i);
        return str;
    }

    private String checkLegality(String urlString){
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException exception) {
            Log.e("MainActivity", "Error with creating URL", exception);
            url = null;
        }
        String result=null;
        // Perform HTTP request to the URL and receive a JSON response back
        try {
            result = makeHttpRequest(url);
            Log.v("string",result);
        } catch (IOException e) {}
        return result;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.connect();
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }
            else
            {
                Log.e("MainActivity",Integer.toString(urlConnection.getResponseCode()));
            }
        }catch (IOException e) {
            Log.e("MainActivity","Exception occured:",e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    @NonNull
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    private void sendEmail() {
        final String username = "jadavshyamal.js@gmail.com";
        final String password = getResources().getString(R.string.mail_Password);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("jadavshyamal.js@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("sarthak734@gmail.com"));
            message.setSubject("Testing Subject");
            message.setText("Dear Mail Crawler,"
                    + "\n\n No spam to my email, please!");


            Multipart multipart = new MimeMultipart();

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            path += "/"+uri.getLastPathSegment();
            String file = path;
            String fileName = "attachmentName";
            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(fileName);
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Done");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String resp;

        @Override
        protected String doInBackground(String... params) {
            if(params.length != 0)
            {

                return checkLegality(params[0]);
            }
            else {
                sendEmail();
                return null;
            }
        }


        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
            if(result == null)
                Toast.makeText(MainActivity.this,"Mail sent",Toast.LENGTH_LONG).show();
            else {
                text.setText(result);
                if (isOnline())
                {
                    AsyncTaskRunner runner = new AsyncTaskRunner();
                    runner.execute();
                }
            }
        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Thank You",
                    "Wait till your request gets processed");
        }


        @Override
        protected void onProgressUpdate(String... text) {
        }
    }
}

