package com.example.app.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FTPFile {
    String name;
    Long size;
    LocalDateTime erstellungszeit;

}
