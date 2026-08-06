package in.esmartsolution.milkflow.repository;

import android.content.Context;

import in.esmartsolution.milkflow.dao.UserDao;
import in.esmartsolution.milkflow.database.AppDatabase;
import in.esmartsolution.milkflow.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executorService;

    public UserRepository(Context context) {

        AppDatabase db = AppDatabase.getInstance(context);

        userDao = db.userDao();

        executorService = Executors.newSingleThreadExecutor();
    }

    // Save User
    public void insertUser(User user) {

        executorService.execute(() -> {

            userDao.insert(user);

        });

    }

    // Get User by Type + Mobile
    public User getUser(
            String type,
            String mobile
    ) {

        return userDao.getUser(
                type,
                mobile
        );

    }

    // STEP 11.2
    // Login User using Type + Mobile + Password
    public User login(
            String type,
            String mobile,
            String password
    ) {

        return userDao.login(
                type,
                mobile,
                password
        );

    }

}