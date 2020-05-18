package com.tunahanozatac.taskmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLData;

public class AddActivity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText taskName, taskDescription, taskTime;
    Button button;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        imageView = findViewById(R.id.imageView);
        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        taskTime = findViewById(R.id.taskTime);
        button = findViewById(R.id.button);

        database = this.openOrCreateDatabase("Task",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.matches("new")){
            taskName.setText("");
            taskDescription.setText("");
            taskTime.setText("");
            button.setVisibility(View.VISIBLE);
            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.task);
            imageView.setImageBitmap(selectImage);

        }else{
            int taskId = intent.getIntExtra("taskId",1);
            button.setVisibility(View.INVISIBLE);

            Cursor cursor = database.rawQuery("SELECT * FROM tasks WHERE id = ?",new String[]{String.valueOf(taskId)}); //veri tabanında gezine biliyoruz.
            int nameIx = cursor.getColumnIndex("name");
            int descriptionIx = cursor.getColumnIndex("description");
            int tasktimeIx = cursor.getColumnIndex("tasktime");
            int imageIx = cursor.getColumnIndex("image");
            while (cursor.moveToNext()){
                taskName.setText(cursor.getString(nameIx));
                taskDescription.setText(cursor.getString(descriptionIx));
                taskTime.setText(cursor.getString(tasktimeIx));
                byte[] bytes = cursor.getBlob(imageIx);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                imageView.setImageBitmap(bitmap);
            }
            cursor.close();

        }

    }

    public void selectTask(View view) {
        //Image'a tıklandıgı zaman yapılacaklar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //izin varmı yokmu kontrol sagladık ve İzin verilmemis ise gerçekleşecek kodlar
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1); //izin istedik.

        }else {
            //İzin verilmis ise gerçekleşecek kodlar
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI); //Galeriye gönderdik
            startActivityForResult(intentToGallery,2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //İzinler istendiginde sonucunda nelerin olacagı
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){//Eleman varsa ve izin vermis ise
                //İzin verilmis ise gerçekleşecek kodlar
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI); //Galeriye gönderdik
                startActivityForResult(intentToGallery,2);
            }else {
                Toast.makeText(getApplicationContext(),"İzin vermedi",Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //startActivityForResult dan dönen sonucu gerçekleştiriyoruz
         if (requestCode == 2 && resultCode == RESULT_OK && data != null){
             Uri imageData = data.getData();
             try {
                 if (Build.VERSION.SDK_INT >= 28){
                     //SDK 28'den büyük ve esitses
                     ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                     selectedImage = ImageDecoder.decodeBitmap(source);
                     imageView.setImageBitmap(selectedImage);

                 }else {
                     selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                     imageView.setImageBitmap(selectedImage);
                 }
             } catch (IOException e) {
                 e.printStackTrace();
             }

         }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void saveTask(View view) {
        //Kaydetme islemi ile yapılacak islemler

        String nameTask = taskName.getText().toString();
        String descriptionTask = taskDescription.getText().toString();
        String timeTask = taskTime.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream); //Resmi kaydetmek için kücülttük.
        byte[] byteArray = outputStream.toByteArray();

        try {
            //Verilerimizi SQLiteDatabaseine koymaya calısıyoruz
            database = this.openOrCreateDatabase("Task",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY, name VARCHAR, description VARCHAR, tasktime VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO tasks(name, description, tasktime, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement statement = database.compileStatement(sqlString); //stringi sql gibi calıstırmaya yarıyor.

            statement.bindString(1, nameTask);
            statement.bindString(2, descriptionTask);
            statement.bindString(3, timeTask);
            statement.bindBlob(4, byteArray);
            statement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(AddActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(Bitmap image, int maxSize){
        //Resim boyutlarını kendimiz bu method ile de kuculttuk
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){
            //Resim  yatay boyutta ise
            width = maxSize;
            height = (int) (width / bitmapRatio);

        }else {
            //Resim dikey boyutta ise
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image,width,height,true);
    }
}
