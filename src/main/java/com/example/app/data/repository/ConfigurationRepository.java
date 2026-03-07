package com.example.app.data.repository;

import com.example.app.data.entity.Configuration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    @Query("Select c from Configuration c where c.userName='Michi'")
    List<Configuration> findByName();

    @Modifying
    @Transactional
    @Query("UPDATE Configuration c SET c.name = :name, c.userName = :userName, c.db_Url = :dbUrl, c.isMonitoring = :isMonitoring, c.isWatchdog = :isWatchdog WHERE c.id = :id")
    void updateWithoutPassword(Long id, String name, String userName, String dbUrl, Integer isMonitoring, Integer isWatchdog);

    @Modifying
    @Transactional
    @Query("UPDATE Configuration c SET c.name = :name, c.userName = :userName, c.password = :password, c.db_Url = :dbUrl, c.isMonitoring = :isMonitoring, c.isWatchdog = :isWatchdog WHERE c.id = :id")
    void updateWithPassword(Long id, String name, String userName, String password, String dbUrl, Integer isMonitoring, Integer isWatchdog );

}
