package com.example.app.utils;

import com.example.app.data.entity.fvm_monitoring;

public interface myCallback {
    void cancel();
    void save(fvm_monitoring mon);
    void delete(fvm_monitoring mon);
}