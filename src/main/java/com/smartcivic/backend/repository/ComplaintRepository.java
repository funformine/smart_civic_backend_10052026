package com.smartcivic.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcivic.backend.model.Complaint;
import com.smartcivic.backend.model.User;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Page<Complaint> findByReporter(User reporter, Pageable pageable);

    List<Complaint> findByReporter(User reporter);

    List<Complaint> findByAssignedSubAdmin(User subAdmin);

    List<Complaint> findByCategory(String category);

    List<Complaint> findByCategoryIgnoreCase(String category);

    List<Complaint> findByCategoryContainingIgnoreCase(String category);

    List<Complaint> findByLocationContainingIgnoreCase(String location);

    List<Complaint> findByStatus(String status);

    long countByReporter(User reporter);

    long countByReporterAndStatus(User reporter, String status);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Complaint c WHERE " +
            "LOWER(c.category) LIKE LOWER(CONCAT('%', :dept, '%')) OR " +
            "LOWER(:dept) LIKE LOWER(CONCAT('%', c.category, '%'))")
    List<Complaint> findByCategoryBidirectional(@org.springframework.data.repository.query.Param("dept") String dept);

    @org.springframework.data.jpa.repository.Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.reporter = ?1 GROUP BY c.status")
    List<Object[]> findStatusCountsByReporter(User reporter);
}
