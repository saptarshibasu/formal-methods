package com.formalmethods.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.formalmethods.verification.domain.VerificationResult;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {

    List<VerificationResult> findByJobIdOrderByCreatedAtDesc(Long jobId);
}
