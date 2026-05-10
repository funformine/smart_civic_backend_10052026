package com.smartcivic.backend.repository;

import com.smartcivic.backend.model.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {
    List<IssueType> findByCategory(String category);

    Optional<IssueType> findByCategoryAndIssueName(String category, String issueName);
}
