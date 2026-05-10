package com.smartcivic.backend.controller;

import com.smartcivic.backend.model.IssueType;
import com.smartcivic.backend.repository.IssueTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/issue-types")
@CrossOrigin(origins = "http://localhost:4200")
public class IssueTypeController {

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @GetMapping("/{category}")
    public List<String> getIssueTypes(@PathVariable String category) {
        return issueTypeRepository.findByCategory(category)
                .stream()
                .map(IssueType::getIssueName)
                .collect(Collectors.toList());
    }

    @PostMapping("/seed")
    public String seedDefaults() {
        if (issueTypeRepository.count() > 0)
            return "Already seeded.";

        seed("Power", "No power supply");
        seed("Power", "Voltage fluctuation");
        seed("Power", "Meter issue");
        seed("Power", "Cable damage");

        seed("Water", "No water supply");
        seed("Water", "Pipeline leakage");
        seed("Water", "Dirty water");
        seed("Water", "Low pressure");

        seed("Land", "Encroachment");
        seed("Land", "Property dispute");
        seed("Land", "Illegal building");
        seed("Land", "Waste dumping");

        return "Seeded successfully.";
    }

    private void seed(String cat, String name) {
        issueTypeRepository.save(IssueType.builder().category(cat).issueName(name).build());
    }
}
