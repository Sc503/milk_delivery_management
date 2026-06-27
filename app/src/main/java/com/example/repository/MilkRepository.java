package com.example.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;

import com.example.dao.PaymentDao;
import com.example.models.Payment;

import com.example.dao.StaffDao;
import com.example.models.Staff;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.firebase.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class MilkRepository {
    private final CustomerDao customerDao;
    private final DeliveryDao deliveryDao;

    private final PaymentDao paymentDao;

    private final StaffDao staffDao;
    private final ExecutorService executorService;

    private final FirebaseFirestore firestore;



    public MilkRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.customerDao = db.customerDao();
        this.deliveryDao = db.deliveryDao();
        this.paymentDao = db.paymentDao();
        this.staffDao = db.staffDao();
        this.executorService = Executors.newFixedThreadPool(4);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    // --- Customer Functions ---

    public void insertCustomer(Customer customer,
                               OnIdReturnedListener listener) {

        executorService.execute(() -> {

            Customer existing =
                    customerDao.getCustomerByMobileSync(
                            customer.getMobile()
                    );

            if (existing != null) {

                if (listener != null) {
                    listener.onError(
                            "Mobile number already exists"
                    );
                }

                return;
            }

            long newId =
                    customerDao.insert(customer);

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

    // --- Delivery Functions ---

    public void recordDelivery(Delivery delivery) {
        executorService.execute(() -> deliveryDao.insert(delivery));
    }

    public void deliverCustomer(
            long customerId,
            String date,
            String time,
            Runnable onCompleted
    ) {
        executorService.execute(() -> {

            Delivery existing =
                    deliveryDao.getDeliveryForCustomerAndDate(
                            customerId,
                            date
                    );

            if (existing == null) {

                Delivery d = new Delivery(
                        customerId,
                        date,
                        time,
                        "Delivered"
                );

                deliveryDao.insert(d);

            } else {

                existing.setStatus("Delivered");
                existing.setDeliveredTime(time);

                deliveryDao.update(existing);

                Delivery check =
                        deliveryDao.getDeliveryForCustomerAndDate(
                                customerId,
                                date
                        );

                Log.d(
                        "DELIVERY_CHECK",
                        check.getDeliveryDate()
                                + " | "
                                + check.getStatus()
                );
            }


            // Firebase Save
            FirebaseFirestore db =
                    FirebaseManager.getDb();

            Map<String, Object> map =
                    new HashMap<>();

            map.put("customerId", customerId);
            map.put("date", date);
            map.put("time", time);
            map.put("status", "Delivered");

            Log.d("FIREBASE_TEST", "Before Firebase Save");

            db.collection("deliveries")
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("FIREBASE_TEST", "Saved to Firebase: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIREBASE_TEST", "Firebase ERROR = " + e.getMessage());
                    });

            Log.d("FIREBASE_TEST", "After Firebase Save");

            if (onCompleted != null) {
                onCompleted.run();
            }
        });

    }

    public void readDeliveriesFromFirebase() {

        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("deliveries")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Log.d("FIREBASE_READ",
                            "Total Docs = "
                                    + queryDocumentSnapshots.size());

                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots.getDocuments()) {

                        Long customerId =
                                doc.getLong("customerId");

                        String date =
                                doc.getString("date");

                        String time =
                                doc.getString("time");

                        String status =
                                doc.getString("status");

                        Log.d(
                                "FIREBASE_READ",
                                customerId + " | "
                                        + date + " | "
                                        + time + " | "
                                        + status
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "FIREBASE_READ",
                            "ERROR = " + e.getMessage()
                    );
                });
    }
    public void markDeliveryPending(long customerId, String date, Runnable onCompleted) {
        executorService.execute(() -> {
            Delivery existing = deliveryDao.getDeliveryForCustomerAndDate(customerId, date);
            if (existing == null) {
                Delivery d = new Delivery(customerId, date, "--", "Pending");
                deliveryDao.insert(d);
            } else {
                existing.setStatus("Pending");
                existing.setDeliveredTime("--");
                deliveryDao.update(existing);
            }
            if (onCompleted != null) {
                onCompleted.run();
            }
        });
    }

    public LiveData<List<Delivery>> getDeliveriesForCustomer(long customerId) {
        return deliveryDao.getDeliveriesForCustomer(customerId);
    }

    public int getDeliveredDaysCount(long customerId){

        return deliveryDao.getDeliveredDaysCount(customerId);

    }
    public List<Customer> getAllCustomersSync() {
        return customerDao.getAllCustomersSync();
    }

    public List<Delivery> getAllDeliveriesSync() {return deliveryDao.getAllDeliveriesSync();}

    public List<Delivery> getDeliveriesForCustomerSync(long customerId) {
        return deliveryDao.getDeliveriesForCustomerSync(customerId);
    }

    public Customer getCustomerByMobileSync(
            String mobile
    ){
        return customerDao.getCustomerByMobile(
                mobile
        );
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

    // ---------------- Payment Functions ----------------

    public Payment getPayment(
            long customerId,
            String month) {

        return paymentDao.getPayment(
                customerId,
                month);

    }

    public List<Payment> getPaymentHistory(
            long customerId){

        return paymentDao
                .getPaymentHistory(customerId);

    }

    public void savePayment(
            Payment payment) {

        executorService.execute(() ->
                paymentDao.insert(payment));

    }

    public void backupPaymentToFirebase(Payment payment) {

        Map<String, Object> data = new HashMap<>();

        data.put("customerId", payment.getCustomerId());
        data.put("month", payment.getMonth());
        data.put("amount", payment.getAmount());
        data.put("status", payment.getStatus());

        firestore.collection("payments")
                .add(data)
                .addOnSuccessListener(doc -> {

                    android.util.Log.d("FIREBASE",
                            "Payment backed up successfully");

                })
                .addOnFailureListener(e -> {

                    android.util.Log.e("FIREBASE",
                            "Backup failed: " + e.getMessage());

                });
    }

    // ---------------- Staff Functions ----------------

    public void insertStaff(Staff staff) {

        executorService.execute(() ->
                staffDao.insert(staff));

    }

    public LiveData<List<Staff>> getAllStaff() {

        return staffDao.getAllStaff();

    }

    public void updateStaff(Staff staff) {

        executorService.execute(() ->
                staffDao.update(staff));

    }

    public void deleteStaff(Staff staff) {

        executorService.execute(() ->
                staffDao.delete(staff));

    }
    public interface OnIdReturnedListener {
        void onIdReturned(long id);
        void onError(String message);
    }
}
