package com.example.firebase;

import android.content.Context;
import android.util.Log;

import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.database.AppDatabase;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executors;

public class DeliverySyncListener {

    private static boolean started = false;

    public static void start(Context context) {

        if (started) {
            return;
        }

        started = true;

        FirebaseFirestore.getInstance()
                .collection("deliveries")
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) {
                        Log.e("SYNC", "Firestore Error", error);
                        return;
                    }

                    for (DocumentChange dc : value.getDocumentChanges()) {

                        if (dc.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }

                        Long customerId =
                                dc.getDocument().getLong("customerId");

                        String date =
                                dc.getDocument().getString("date");

                        String time =
                                dc.getDocument().getString("time");

                        String status =
                                dc.getDocument().getString("status");

                        if (time == null) {
                            time = "--";
                        }

                        if (status == null) {
                            status = "Pending";
                        }

                        if (customerId == null || date == null) {
                            continue;
                        }

                        final String finalTime = time;
                        final String finalStatus = status;

                        Executors.newSingleThreadExecutor().execute(() -> {

                            try {

                                AppDatabase db =
                                        AppDatabase.getInstance(context);

                                DeliveryDao deliveryDao =
                                        db.deliveryDao();

                                CustomerDao customerDao =
                                        db.customerDao();

                                Log.d(
                                        "SYNC",
                                        "Firebase CustomerId = " + customerId
                                );

                                Customer customer =
                                        customerDao.getCustomerByIdSync(
                                                customerId
                                        );

                                if (customer == null) {

                                    Log.e(
                                            "SYNC",
                                            "Customer not found. ID = "
                                                    + customerId
                                    );

                                    return;
                                }

                                Delivery existing =
                                        deliveryDao.findDelivery(
                                                customerId,
                                                date
                                        );

                                if (existing == null) {

                                    Delivery delivery =
                                            new Delivery(
                                                    customerId,
                                                    date,
                                                    finalTime,
                                                    finalStatus
                                            );

                                    deliveryDao.insert(delivery);

                                    Log.d(
                                            "SYNC",
                                            "Delivery inserted: "
                                                    + customerId
                                    );

                                } else {

                                    existing.setStatus(finalStatus);
                                    existing.setDeliveredTime(finalTime);

                                    deliveryDao.update(existing);

                                    Log.d(
                                            "SYNC",
                                            "Delivery updated: "
                                                    + customerId
                                    );
                                }

                            } catch (Exception e) {

                                Log.e(
                                        "SYNC",
                                        "SYNC ERROR = "
                                                + e.getMessage(),
                                        e
                                );
                            }
                        });
                    }
                });
    }
}