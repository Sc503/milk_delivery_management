package com.example.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.dao.CustomerDao;
import com.example.dao.UserDao;
import com.example.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.models.Payment;
import com.example.dao.PaymentDao;
import com.example.dao.DeliveryDao;

import com.example.dao.StaffDao;
import com.example.models.Staff;


@Database(
        entities = {
                Customer.class,
                Delivery.class,
                Payment.class,
                User.class,
                Staff.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CustomerDao customerDao();
    public abstract DeliveryDao deliveryDao();
    public abstract UserDao userDao();

    public abstract PaymentDao paymentDao();

    public abstract StaffDao staffDao();

    public static AppDatabase getInstance(Context context) {

        if (instance == null) {

            synchronized (AppDatabase.class) {

                if (instance == null) {

                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "milk_delivery_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    // 🔥 SAFE PRELOAD DATA
    private static final RoomDatabase.Callback roomCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    ExecutorService executor = Executors.newSingleThreadExecutor();

                    executor.execute(() -> {

                        if (instance == null) return;

                        UserDao dao = instance.userDao();


                    });
                }
            };


}