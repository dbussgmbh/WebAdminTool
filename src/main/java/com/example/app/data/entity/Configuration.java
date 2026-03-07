package com.example.app.data.entity;

import com.example.app.utils.AesPasswordUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
@Entity
@Table(name = "SQL_CONFIGURATION")
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME")
    private String name="";

    @Column(name = "USER_NAME")
    private String userName="";

    private String password="";

    private String db_Url="";

    @Column(name = "IS_MONITORING")
    private Integer isMonitoring;

    @Column(name = "IS_WATCHDOG")
    private Integer isWatchdog;

    @Column(name = "ACCESS_ROLES")
    private String access_roles;

    public static boolean isEncryptedPassword(String value) {
        return AesPasswordUtil.isEncrypted(value);
    }

    public static String encryptPassword(String plainPassword, String secret) {
        return AesPasswordUtil.encrypt(plainPassword, secret);
    }

    public static String decodePassword(String storedPassword, String secret) {
        return AesPasswordUtil.decrypt(storedPassword, secret);
    }
}
