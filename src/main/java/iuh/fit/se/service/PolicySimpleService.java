// src/main/java/iuh/fit/se/service/PolicySimpleService.java
package iuh.fit.se.service;

import iuh.fit.se.entity.Policy;
import iuh.fit.se.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PolicySimpleService {

    private final PolicyRepository repo;

    public Optional<Policy> getByCode(String code) {
        return repo.findByCode(code);
    }

    public List<Policy> search(String q, int limit, boolean useFulltext) {
        if (q == null || q.isBlank()) return List.of();
        if (useFulltext) {
            return repo.searchFulltext(q, Math.max(1, limit));
        }
        var all = repo.searchLike(q);
        return all.stream().limit(Math.max(1, limit)).toList();
    }

    public List<Policy> listAllNewestFirst(int limit) {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Policy::getEffectiveDate).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }
    public List<Policy> getAll() {
        return repo.findAll();
    }
}
