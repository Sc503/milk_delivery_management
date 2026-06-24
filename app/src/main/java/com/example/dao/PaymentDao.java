package com.example.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;


import com.example.models.Payment;


import com.example.models.Payment;

import java.util.List;

@Dao
public interface PaymentDao {

    @Query(
            "SELECT * FROM payments " +
                    "WHERE customerId=:customerId " +
                    "AND month=:month LIMIT 1")
    Payment getPayment(long customerId, String month);

    @Insert
    void insert(Payment payment);

    @Query(
            "SELECT * FROM payments " +
                    "WHERE customerId=:customerId " +
                    "ORDER BY month DESC")
    List<Payment> getPaymentHistory(long customerId);

}