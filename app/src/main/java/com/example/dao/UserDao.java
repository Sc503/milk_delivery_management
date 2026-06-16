package com.example.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.models.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM users WHERE userType=:type AND mobile=:mobile LIMIT 1")
    User getUser(String type,String mobile);

    @Query("SELECT * FROM users WHERE userType=:type AND mobile=:mobile AND password=:password LIMIT 1")
    User login(String type, String mobile, String password);

    

}