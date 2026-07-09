package com.example.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.models.DeliveryWithStaff;
import com.example.models.Payment;
import com.example.models.Staff;
import com.example.repository.MilkRepository;

import java.util.List;

public class MilkViewModel extends AndroidViewModel {
    private final MilkRepository repository;

    public MilkViewModel(@NonNull Application application) {
        super(application);
        repository = new MilkRepository(application);
    }

    public MilkRepository getRepository() {
        return repository;
    }

    // --- Customer Operations ---

    public LiveData<List<Customer>> getAllCustomers() {
        return repository.getAllCustomers();
    }

    public Customer getCustomerByIdSync(long id) {
        return repository.getCustomerByIdSync(id);
    }

    public LiveData<Customer> getCustomerById(long id) {
        return repository.getCustomerById(id);
    }

    public LiveData<List<Customer>> getPendingCustomersForDate(String todayDate) {
        return repository.getPendingCustomersForDate(todayDate);
    }

    public void insertCustomer(Customer customer, MilkRepository.OnIdReturnedListener listener) {
        repository.insertCustomer(customer, listener);
    }

    public void updateCustomer(Customer customer) {
        repository.updateCustomer(customer);
    }

    public void deleteCustomer(Customer customer) {
        repository.deleteCustomer(customer);
    }

    // --- Delivery Operations ---

    //  UPDATED: accept staffId
    public void deliverCustomer(long customerId, String date, String time, long staffId, Runnable onCompleted) {
        repository.deliverCustomer(customerId, date, time, staffId, onCompleted);
    }

    // KEEP OLD METHOD for backward compatibility
    public void deliverCustomer(long customerId, String date, String time, Runnable onCompleted) {
        repository.deliverCustomer(customerId, date, time, 0, onCompleted);
    }

    public void markDeliveryPending(long customerId, String date, Runnable onCompleted) {
        repository.markDeliveryPending(customerId, date, onCompleted);
    }

    public LiveData<List<Delivery>> getDeliveriesForCustomer(long customerId) {
        return repository.getDeliveriesForCustomer(customerId);
    }

    public int getDeliveredDaysCount(long customerId, String month) {
        return repository.getDeliveredDaysCount(customerId, month);
    }

    public LiveData<List<Delivery>> getDeliveriesForDate(String date) {
        return repository.getDeliveriesForDate(date);
    }

    //  Get delivery with staff name
    public DeliveryWithStaff getDeliveryWithStaff(long customerId, String date) {
        return repository.getDeliveryWithStaff(customerId, date);
    }

    // --- Payment Operations ---

    public Payment getPayment(long customerId, String month) {
        return repository.getPayment(customerId, month);
    }

    public List<Payment> getPaymentHistory(long customerId) {
        return repository.getPaymentHistory(customerId);
    }

    public void savePayment(Payment payment) {
        repository.savePayment(payment);
    }

    public List<Customer> getAllCustomersSync() {
        return repository.getAllCustomersSync();
    }

    // --- Staff Operations ---

    public void insertStaff(Staff staff) {
        repository.insertStaff(staff);
    }

    public LiveData<List<Staff>> getAllStaff() {
        return repository.getAllStaff();
    }

    public void updateStaff(Staff staff) {
        repository.updateStaff(staff);
    }

    public void deleteStaff(Staff staff) {
        repository.deleteStaff(staff);
    }

    public void resetAllToPending() {
        repository.resetAllToPending();
    }
}