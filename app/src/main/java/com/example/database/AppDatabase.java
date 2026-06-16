package com.example.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.dao.CustomerDao;
import com.example.dao.DeliveryDao;
import com.example.dao.PaymentDao;
import com.example.dao.UserDao;
import com.example.models.Customer;
import com.example.models.Delivery;
import com.example.models.Payment;
import com.example.models.User;

@Database(
        entities = {
                Customer.class,
                Delivery.class,
                Payment.class,
                User.class
        },
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CustomerDao customerDao();

    public abstract DeliveryDao deliveryDao();

    public abstract PaymentDao paymentDao();

    public abstract UserDao userDao();

    public static AppDatabase getInstance(final Context context) {

        if (instance == null) {

            synchronized (AppDatabase.class) {

                if (instance == null) {

                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "milk_delivery_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(roomCallback)
                            .build();
                }
            }
        }

        return instance;
    }

    private static final RoomDatabase.Callback roomCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                }
            };

    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {

                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL(
                            "ALTER TABLE customers ADD COLUMN notes TEXT DEFAULT ''");
                }
            };
}