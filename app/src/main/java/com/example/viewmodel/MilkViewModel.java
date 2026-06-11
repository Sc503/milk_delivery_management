package com.example.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.models.Customer;
import com.example.models.Delivery;
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

    public void deliverCustomer(long customerId, String date, String time, Runnable onCompleted) {
        repository.deliverCustomer(customerId, date, time, onCompleted);
    }
    public void readDeliveriesFromFirebase() {
        repository.readDeliveriesFromFirebase();
    }
    public void markDeliveryPending(long customerId, String date, Runnable onCompleted) {
        repository.markDeliveryPending(customerId, date, onCompleted);
    }

    public LiveData<List<Delivery>> getDeliveriesForCustomer(long customerId) {
        return repository.getDeliveriesForCustomer(customerId);
    }

    public LiveData<List<Delivery>> getDeliveriesForDate(String date) {
        return repository.getDeliveriesForDate(date);
    }
}
