package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

import java.util.ArrayList;
import java.util.List;

public class DataSaved extends AppCompatActivity {

    ListView list;
    Button delete_bt;
    ArrayAdapter<String> adapter;

    String id[] = {""};

    Intent i;

    private DataEcgDatabase ecg_database;
    private DataEcgDatabase getDatabaseManager() {
        if (ecg_database==null)
            ecg_database=DataEcgDatabase.getDatabase(this);
        return ecg_database;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_saved_screen);


        list = (ListView) findViewById(R.id.list_data);
        delete_bt = (Button) findViewById(R.id.button);

        delete_bt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getDatabaseManager().noteModel().deleteAll();
                refresh();
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                i = new Intent(DataSaved.this,AnalizeData.class);
                i.putExtra("id",position+1); //position parte da 0
                DataSaved.this.startActivityForResult(i,1);
            }

        });

        load_id();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            refresh();
        }
    }

    public void refresh() {
        Intent refresh = new Intent(this, DataSaved.class);
        startActivity(refresh);
        this.finish();
    }

    public void load_id() {

        List<String> u = new ArrayList<String>();

        int size = getDatabaseManager().noteModel().loadAllNotes().size();
        String d;

        for (long i = 1;i<=size;i++) {
            if (getDatabaseManager().noteModel().loadNote(i).start_time!=null) {
                d = getDatabaseManager().noteModel().loadNote(i).start_time;
                d = d.substring(0, d.length() - 15);
                u.add(d);
            }
        }

        //if (size<1)
        //    System.out.println("Nessun dato da visulizzare");

        adapter=new ArrayAdapter<String>(this, R.layout.list_view,R.id.textViewList,u);

        list.setAdapter(adapter);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


}