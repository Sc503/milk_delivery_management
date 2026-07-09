package com.example.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

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

    //  ADD DELETE METHOD - Option 1: Delete by customerId and month
    @Query("DELETE FROM payments WHERE customerId = :customerId AND month = :month")
    void deletePayment(long customerId, String month);

    //  ADD DELETE METHOD - Option 2: Delete by Payment object (if you prefer)
    @Delete
    void delete(Payment payment);

    //  Optional: Delete all payments for a customer
    @Query("DELETE FROM payments WHERE customerId = :customerId")
    void deleteAllPaymentsForCustomer(long customerId);
}