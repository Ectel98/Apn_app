package com.example.splashscreen.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataEcgDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DataEcg note);

    @Query("SELECT * FROM DataEcg")
    List<DataEcg> loadAllNotes();

    @Query("SELECT * FROM DataEcg WHERE id=:id")
    DataEcg loadNote(long id);

    @Delete
    void deleteNote(DataEcg n);

    @Query("DELETE FROM DataEcg")
    void deleteAll();

    @Query("DELETE FROM DataEcg WHERE id=:id")
    void deleteById(long id);

}
