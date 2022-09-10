package com.example.splashscreen.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = DataEcg.class, version = 1,exportSchema = false)
public abstract class DataEcgDatabase extends RoomDatabase {

    private static DataEcgDatabase INSTANCE;
    public abstract DataEcgDao noteModel();

    public static DataEcgDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), DataEcgDatabase.class, "note_db").allowMainThreadQueries().build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

}
