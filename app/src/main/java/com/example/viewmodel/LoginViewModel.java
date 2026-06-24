package com.example.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.models.User;
import com.example.repository.UserRepository;

public class LoginViewModel extends AndroidViewModel {

    private final UserRepository repository;

    public LoginViewModel(@NonNull Application application) {
        super(application);

        repository = new UserRepository(application);
    }

    // Save New User
    public void insertUser(User user) {

        repository.insertUser(user);

    }

    // Get Existing User
    public User getUser(
            String type,
            String mobile
    ) {

        return repository.getUser(
                type,
                mobile
        );

    }

    // STEP 11.3
    // Login User using Type + Mobile + Password
    public User login(
            String type,
            String mobile,
            String password
    ) {

        return repository.login(
                type,
                mobile,
                password
        );

    }

}