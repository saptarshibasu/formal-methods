package com.formalmethods.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;

@Repository
public interface VerificationJobRepository extends JpaRepository<VerificationJob, Long> {

    List<VerificationJob> findBySensorType(SensorType sensorType);

    List<VerificationJob> findByStatus(JobStatus status);

    List<VerificationJob> findBySensorTypeAndStatus(@Param("sensorType") SensorType sensorType,
                                                      @Param("status") JobStatus status);
}
