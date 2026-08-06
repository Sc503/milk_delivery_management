package in.esmartsolution.milkflow.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import in.esmartsolution.milkflow.dao.CustomerDao;
import in.esmartsolution.milkflow.dao.UserDao;
import in.esmartsolution.milkflow.models.Attendance;
import in.esmartsolution.milkflow.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.models.Delivery;
import in.esmartsolution.milkflow.models.Payment;
import in.esmartsolution.milkflow.dao.PaymentDao;
import in.esmartsolution.milkflow.dao.DeliveryDao;
import in.esmartsolution.milkflow.dao.AttendanceDao;

import in.esmartsolution.milkflow.dao.StaffDao;
import in.esmartsolution.milkflow.models.Staff;


@Database(
        entities = {
                Customer.class,
                Delivery.class,
                Payment.class,
                User.class,
                Staff.class,
                Attendance.class,
        },
        version = 11,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CustomerDao customerDao();
    public abstract DeliveryDao deliveryDao();
    public abstract UserDao userDao();

    public abstract PaymentDao paymentDao();

    public abstract StaffDao staffDao();



    public abstract AttendanceDao attendanceDao();

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

    //  SAFE PRELOAD DATA
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