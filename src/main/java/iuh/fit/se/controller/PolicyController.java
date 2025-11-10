package iuh.fit.se.controller;

import iuh.fit.se.dto.ApiResponse;
import iuh.fit.se.entity.Policy;
import iuh.fit.se.service.PolicySimpleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class PolicyController {
    PolicySimpleService policySimpleService;
    @GetMapping("/policies/latest")
    ApiResponse<List<Policy>> getLatestPolicy() {
        return ApiResponse.<List<Policy>>builder()
                .code(200)
                .result(policySimpleService.getAll())
                .message("Latest policy retrieved successfully")
                .build();
    }
}
