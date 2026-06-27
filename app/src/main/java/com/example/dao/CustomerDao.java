package com.example.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.models.Customer;

import java.util.List;

@Dao
public interface CustomerDao {

    @Insert
    long insert(Customer customer);

    @Update
    void update(Customer customer);

    @Delete
    void delete(Customer customer);


    @Query("SELECT * FROM customers ORDER BY name ASC")
    LiveData<List<Customer>> getAllCustomers();

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    LiveData<Customer> getCustomerById(long id);

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    Customer getCustomerByIdSync(long id);

    @Query("SELECT * FROM customers ORDER BY name ASC")
    List<Customer> getAllCustomersSync();

    @Query("SELECT * FROM customers")
    List<Customer> getAllCustomersForBackup();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Customer> customers);

    @Query("DELETE FROM customers")
    void deleteAll();

    @Query("SELECT * FROM customers WHERE mobile = :mobile LIMIT 1")
    Customer getCustomerByMobile(String mobile);

    @Query("SELECT * FROM customers WHERE mobile = :mobile LIMIT 1")
    Customer getCustomerByMobileSync(String mobile);

    @Query("SELECT c.* FROM customers c " +
            "LEFT JOIN deliveries d ON c.id = d.customerId AND d.deliveryDate = :todayDate " +
            "WHERE d.id IS NULL OR d.status = 'Pending' " +
            "ORDER BY c.name ASC")
    LiveData<List<Customer>> getPendingCustomersForDate(String todayDate);


}