package com.example.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.models.DeliveryWithStaff;
import com.example.dao.PaymentDao;
import com.example.models.Payment;
import com.example.dao.StaffDao;
import com.example.models.Staff;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MilkRepository {
    private final CustomerDao customerDao;
    private final DeliveryDao deliveryDao;
    private final PaymentDao paymentDao;
    private final StaffDao staffDao;
    private final ExecutorService executorService;

    public MilkRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.customerDao = db.customerDao();
        this.deliveryDao = db.deliveryDao();
        this.paymentDao = db.paymentDao();
        this.staffDao = db.staffDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    // --- Customer Functions ---

    public void insertCustomer(Customer customer, OnIdReturnedListener listener) {
        executorService.execute(() -> {
            Customer existing = customerDao.getCustomerByMobileSync(customer.getMobile());
            if (existing != null) {
                if (listener != null) {
                    listener.onError("Mobile number already exists");
                }
                return;
            }
            long newId = customerDao.insert(customer);
            if (listener != null) {
                listener.onIdReturned(newId);
            }
        });
    }

    public void updateCustomer(Customer customer) {
        executorService.execute(() -> customerDao.update(customer));
    }

    public void deleteCustomer(Customer customer) {
        executorService.execute(() -> customerDao.delete(customer));
    }

    public LiveData<List<Customer>> getAllCustomers() {
        return customerDao.getAllCustomers();
    }

    public LiveData<Customer> getCustomerById(long id) {
        return customerDao.getCustomerById(id);
    }

    public Customer getCustomerByIdSync(long id) {
        return customerDao.getCustomerByIdSync(id);
    }

    public LiveData<List<Customer>> getPendingCustomersForDate(String todayDate) {
        return customerDao.getPendingCustomersForDate(todayDate);
    }

    public List<Customer> getAllCustomersSync() {
        return customerDao.getAllCustomersSync();
    }

    public Customer getCustomerByMobileSync(String mobile) {
        return customerDao.getCustomerByMobile(mobile);
    }



    //  accept staffId
    public void deliverCustomer(long customerId, String date, String time, long staffId, Runnable onCompleted) {
        executorService.execute(() -> {
            Delivery existing = deliveryDao.getDeliveryForCustomerAndDate(customerId, date);

            if (existing == null) {
                Delivery d = new Delivery(customerId, date, time, "Delivered", staffId);
                deliveryDao.insert(d);
                Log.d("DELIVERY_SAVE", " New delivery saved: " + date + " -> Delivered by staff: " + staffId);
            } else {
                existing.setStatus("Delivered");
                existing.setDeliveredTime(time);
                existing.setStaffId(staffId);
                deliveryDao.update(existing);
                Log.d("DELIVERY_SAVE", " Delivery updated: " + date + " -> Delivered by staff: " + staffId);
            }

            // Verify it was saved
            Delivery check = deliveryDao.getDeliveryForCustomerAndDate(customerId, date);
            if (check != null) {
                Log.d("DELIVERY_VERIFY", "Verified: " + check.getDeliveryDate() + " | " + check.getStatus() + " | Staff: " + check.getStaffId());
            }

            if (onCompleted != null) {
                onCompleted.run();
            }
        });
    }


    public void deliverCustomer(long customerId, String date, String time, Runnable onCompleted) {
        deliverCustomer(customerId, date, time, 0, onCompleted);
    }

    public void markDeliveryPending(long customerId, String date, Runnable onCompleted) {
        executorService.execute(() -> {
            Delivery existing = deliveryDao.getDeliveryForCustomerAndDate(customerId, date);

            if (existing == null) {
                Delivery d = new Delivery(customerId, date, "--", "Pending");
                deliveryDao.insert(d);
                Log.d("DELIVERY_SAVE", " New delivery saved: " + date + " -> Pending");
            } else {
                existing.setStatus("Pending");
                existing.setDeliveredTime("--");
                existing.setStaffId(0); // Reset staff when pending
                deliveryDao.update(existing);
                Log.d("DELIVERY_SAVE", "Delivery updated: " + date + " -> Pending");
            }

            // Verify it was saved
            Delivery check = deliveryDao.getDeliveryForCustomerAndDate(customerId, date);
            if (check != null) {
                Log.d("DELIVERY_VERIFY", " Verified: " + check.getDeliveryDate() + " | " + check.getStatus());
            }

            if (onCompleted != null) {
                onCompleted.run();
            }
        });
    }

    public LiveData<List<Delivery>> getDeliveriesForCustomer(long customerId) {
        return deliveryDao.getDeliveriesForCustomer(customerId);
    }

    public List<Delivery> getDeliveriesForCustomerSync(long customerId) {
        return deliveryDao.getDeliveriesForCustomerSync(customerId);
    }

    public int getDeliveredDaysCount(long customerId, String month) {
        return deliveryDao.getDeliveredDaysCount(customerId, month);
    }

    public List<Delivery> getAllDeliveriesSync() {
        return deliveryDao.getAllDeliveriesSync();
    }

    public List<Delivery> getDeliveriesForMonthSync(String yearMonthPrefix) {
        return deliveryDao.getDeliveriesForMonthSync(yearMonthPrefix);
    }

    public List<Delivery> getDeliveriesForDateSync(String date) {
        return deliveryDao.getDeliveriesForDateSync(date);
    }

    public LiveData<List<Delivery>> getDeliveriesForDate(String date) {
        return deliveryDao.getDeliveriesForDate(date);
    }

    //  Get delivery with staff name
    public DeliveryWithStaff getDeliveryWithStaff(long customerId, String date) {
        return deliveryDao.getDeliveryWithStaff(customerId, date);
    }

    // --- Payment Functions ---

    public Payment getPayment(long customerId, String month) {
        return paymentDao.getPayment(customerId, month);
    }

    public List<Payment> getPaymentHistory(long customerId) {
        return paymentDao.getPaymentHistory(customerId);
    }

    public void savePayment(Payment payment) {
        executorService.execute(() -> paymentDao.insert(payment));
    }

    // --- Staff Functions ---

    public void insertStaff(Staff staff) {
        executorService.execute(() -> staffDao.insert(staff));
    }

    public LiveData<List<Staff>> getAllStaff() {
        return staffDao.getAllStaff();
    }

    public void updateStaff(Staff staff) {
        executorService.execute(() -> staffDao.update(staff));
    }

    public void deleteStaff(Staff staff) {
        executorService.execute(() -> staffDao.delete(staff));
    }

    // --- Interface ---

    public interface OnIdReturnedListener {
        void onIdReturned(long id);
        void onError(String message);
    }

    public void resetAllToPending() {
        executorService.execute(deliveryDao::resetAllToPending);
    }
}