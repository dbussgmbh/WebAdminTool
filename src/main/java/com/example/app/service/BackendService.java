package com.example.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class BackendService {
    @Autowired
    JdbcTemplate jdbcTemplate;


    public void save(String name) {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public CompletableFuture<Integer> saveAsync(String name) {
        int i=0;

           // Thread.sleep(10);
            Random random = new Random();
            i = random.nextInt(300);

        return CompletableFuture.completedFuture(i);
    }



}
