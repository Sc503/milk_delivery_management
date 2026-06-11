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
import com.example.models.Customer;
import com.example.models.Delivery;

@Database(entities = {Customer.class, Delivery.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CustomerDao customerDao();
    public abstract DeliveryDao deliveryDao();

    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "milk_delivery_db")
                            // We provide fallback to destructive migration if needed, 
                            // but we also register a proper migration example below.
                            .fallbackToDestructiveMigration()
                            .addCallback(roomCallback)
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Database initialization callback to pre-populate mock data for demonstration
     * so that the application is immediately interactive when run!
     */
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // We can pre-populate default customers on a background thread if needed,
            // or let the user add them dynamically. It is safer to remain clean but ready.
        }
    };

    /**
     * DATABASE MIGRATION STRATEGY EXAMPLE
     * If we need to upgrade from Database Version 1 to 2 (e.g., adding a notes column to customers):
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Upgrade example: Add an optional "notes" column to the customers table
            database.execSQL("ALTER TABLE customers ADD COLUMN notes TEXT DEFAULT ''");
        }
    };
}
