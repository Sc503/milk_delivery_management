package com.example.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {

    public static FirebaseFirestore db;

    public static FirebaseFirestore getDb() {

        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}