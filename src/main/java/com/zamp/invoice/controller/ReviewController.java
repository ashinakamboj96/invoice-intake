package com.zamp.invoice.controller;

import com.zamp.invoice.dto.CompleteReviewRequest;
import com.zamp.invoice.dto.CompleteReviewResponse;
import com.zamp.invoice.dto.ValidationFailureDto;
import com.zamp.invoice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices/{id}/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/failures")
    public ResponseEntity<List<ValidationFailureDto>> getPendingFailures(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(reviewService.getPendingFailures(id));
    }

    @PostMapping
    public ResponseEntity<CompleteReviewResponse> completeReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CompleteReviewRequest request) {
        return ResponseEntity.ok(reviewService.completeReview(id, request));
    }
}
