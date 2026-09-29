package com.fyp.iotbaseline.repository;

import com.fyp.iotbaseline.model.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByDeviceIdOrderByTimestampDesc(String deviceId);

    Optional<SensorReading> findFirstByDeviceIdOrderByTimestampDesc(String deviceId);

    @Query("SELECT s FROM SensorReading s WHERE s.deviceId = :deviceId " +
           "AND s.timestamp BETWEEN :from AND :to ORDER BY s.timestamp DESC")
    List<SensorReading> findByDeviceIdAndTimeRange(
            @Param("deviceId") String deviceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT s FROM SensorReading s WHERE s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SensorReading> findRecentReadings(@Param("since") LocalDateTime since);

    Page<SensorReading> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<SensorReading> findByDeviceIdOrderByTimestampDesc(String deviceId, Pageable pageable);

    long countByDeviceId(String deviceId);

    long countByQuality(String quality);

    @Query("SELECT s FROM SensorReading s WHERE s.quality = 'VALID' " +
           "AND s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SensorReading> findValidReadingsSince(@Param("since") LocalDateTime since);

    @Query("SELECT s FROM SensorReading s WHERE s.deviceId = :deviceId " +
           "AND s.quality = 'VALID' ORDER BY s.timestamp DESC")
    List<SensorReading> findValidByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    Optional<SensorReading> findFirstByOrderByTimestampDesc();
}
