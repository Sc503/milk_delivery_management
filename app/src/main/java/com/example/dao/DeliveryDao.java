package com.example.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.models.Delivery;

import java.util.List;

@Dao
public interface DeliveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Delivery delivery);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Delivery delivery);

    @Delete
    void delete(Delivery delivery);

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND deliveryDate = :date LIMIT 1")
    Delivery getDeliveryForCustomerAndDate(long customerId, String date);

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY deliveryDate DESC")
    LiveData<List<Delivery>> getDeliveriesForCustomer(long customerId);

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY deliveryDate DESC")
    List<Delivery> getDeliveriesForCustomerSync(long customerId);

    @Query("SELECT * FROM deliveries WHERE deliveryDate LIKE :yearMonthPrefix ORDER BY deliveryDate ASC")
    List<Delivery> getDeliveriesForMonthSync(String yearMonthPrefix);

    @Query("SELECT * FROM deliveries WHERE deliveryDate = :date")
    List<Delivery> getDeliveriesForDateSync(String date);

    @Query("SELECT * FROM deliveries WHERE deliveryDate = :date")
    LiveData<List<Delivery>> getDeliveriesForDate(String date);

    @Query("SELECT * FROM deliveries ORDER BY deliveryDate DESC")
    LiveData<List<Delivery>> getAllDeliveries();

    @Query("SELECT * FROM deliveries")
    List<Delivery> getAllDeliveriesSync();

    @Query("SELECT * FROM deliveries")
    List<Delivery> getAllDeliveriesForBackup();

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND deliveryDate = :date LIMIT 1")
    Delivery findDelivery(long customerId, String date);

    @Query("SELECT COUNT(*) FROM deliveries " +
            "WHERE customerId=:customerId " +
            "AND status='Delivered' " +
            "AND deliveryDate LIKE :month || '%'")
    int getDeliveredDaysCount(long customerId, String month);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Delivery> deliveries);

    @Query("DELETE FROM deliveries")
    void deleteAll();
}
