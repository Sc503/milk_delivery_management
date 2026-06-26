package com.example.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.models.Staff;

import java.util.List;

@Dao
public interface StaffDao {

    @Insert
    long insert(Staff staff);

    @Update
    void update(Staff staff);

    @Delete
    void delete(Staff staff);

    @Query("SELECT * FROM staff ORDER BY name ASC")
    LiveData<List<Staff>> getAllStaff();

    @Query("SELECT * FROM staff WHERE id=:id LIMIT 1")
    Staff getStaffById(long id);


}