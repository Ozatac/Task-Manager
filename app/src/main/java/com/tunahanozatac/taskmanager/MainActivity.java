package com.tunahanozatac.taskmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> nameArray;
    ArrayList<Integer> idArray;
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        nameArray = new ArrayList<String>();
        idArray = new ArrayList<Integer>();

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,nameArray); //listeye baglamamız gerekiyor
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Listview da tıklandıgı zaman gerceklesecek kodlar
                Intent intent = new Intent(MainActivity.this,AddActivity.class);
                intent.putExtra("taskId",idArray.get(position));
                intent.putExtra("info","old");

                startActivity(intent);
            }
        });
        getData();
    }

    public void getData(){
        try {
            SQLiteDatabase database = this.openOrCreateDatabase("Task", MODE_PRIVATE,null);//Veri yoksa tabanı olusturduk varsa actık
            Cursor cursor = database.rawQuery("SELECT * FROM tasks",null); //veri tabanında gezine biliyoruz.
            int nameIx = cursor.getColumnIndex("name");
            int idIx = cursor.getColumnIndex("id");
            while (cursor.moveToNext()){
                nameArray.add(cursor.getString(nameIx));
                idArray.add(cursor.getInt(idIx));
            }
            adapter.notifyDataSetChanged();
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Hangi menunun gösterilmesi gerektigi bu kısımda belirledik.
        //XML dosyalarını activity içerisinde göstere bilmemiz için inflater yapmamız gerekiyor

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.add_task,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //Kullanıcı hangi itemi seçerse ne yapılması gerekenler
        if (item.getItemId() == R.id.add_task){
            Intent intent = new Intent(MainActivity.this,AddActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
