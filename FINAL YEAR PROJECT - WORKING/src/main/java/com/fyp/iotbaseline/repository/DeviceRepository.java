package com.fyp.iotbaseline.repository;

import com.fyp.iotbaseline.model.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceRegistration, String> {

    Optional<DeviceRegistration> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    List<DeviceRegistration> findByStatus(String status);

    @Query("SELECT d FROM DeviceRegistration d WHERE d.lastSeen >= :since")
    List<DeviceRegistration> findActiveDevicesSince(LocalDateTime since);

    @Query("SELECT COUNT(d) FROM DeviceRegistration d WHERE d.lastSeen >= :since")
    long countActiveDevicesSince(LocalDateTime since);
}
