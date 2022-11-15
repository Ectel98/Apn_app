package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

public class Splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ConnectionScreen.class);
        startActivity(intent);
        finish();

    }

}