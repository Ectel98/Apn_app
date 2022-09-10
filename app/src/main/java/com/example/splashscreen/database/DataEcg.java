package com.example.splashscreen.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DataEcg {

    @PrimaryKey(autoGenerate = false)
    public long id;

    @ColumnInfo(name = "START_TIME")
    public String start_time;

    @ColumnInfo(name = "END_TIME")
    public String end_time;

    @ColumnInfo(name = "NUMBER_DATA")
    public long number_data;

    @ColumnInfo(name = "DATA")
    public String data;
}
