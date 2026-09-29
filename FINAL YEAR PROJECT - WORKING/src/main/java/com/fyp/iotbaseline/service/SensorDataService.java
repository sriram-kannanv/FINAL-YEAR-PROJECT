package com.fyp.iotbaseline.service;

import com.fyp.iotbaseline.model.SensorReading;
import com.fyp.iotbaseline.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for storing and retrieving sensor data.
 *
 * <p>Data is stored in plaintext SQLite — no source-level encryption.
 * This is the conventional baseline behaviour documented in Chapter 3.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorDataService {

    private final SensorReadingRepository sensorReadingRepository;

    @Transactional
    public SensorReading save(SensorReading reading) {
        SensorReading saved = sensorReadingRepository.save(reading);
        log.debug("Saved reading id={} device={} temp={}°C hum={}%",
                saved.getId(), saved.getDeviceId(),
                saved.getTemperature(), saved.getHumidity());
        return saved;
    }

    public Optional<SensorReading> getLatest(String deviceId) {
        return sensorReadingRepository.findFirstByDeviceIdOrderByTimestampDesc(deviceId);
    }

    public Optional<SensorReading> getLatestAny() {
        return sensorReadingRepository.findFirstByOrderByTimestampDesc();
    }

    public List<SensorReading> getHistory(String deviceId, LocalDateTime from, LocalDateTime to) {
        return sensorReadingRepository.findByDeviceIdAndTimeRange(deviceId, from, to);
    }

    public List<SensorReading> getRecentReadings(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return sensorReadingRepository.findRecentReadings(since);
    }

    public List<SensorReading> getValidReadingsSince(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return sensorReadingRepository.findValidReadingsSince(since);
    }

    public Page<SensorReading> getAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sensorReadingRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public Page<SensorReading> getByDevicePaged(String deviceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sensorReadingRepository.findByDeviceIdOrderByTimestampDesc(deviceId, pageable);
    }

    public long getTotalCount() {
        return sensorReadingRepository.count();
    }

    public long getCountByDevice(String deviceId) {
        return sensorReadingRepository.countByDeviceId(deviceId);
    }

    public long getRejectedCount() {
        return sensorReadingRepository.countByQuality("AUTH_FAILED") +
               sensorReadingRepository.countByQuality("INVALID_RANGE");
    }
}
