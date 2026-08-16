package com.zamp.invoice.controller;

import com.zamp.invoice.model.dto.CompleteReviewRequest;
import com.zamp.invoice.model.dto.CompleteReviewResponse;
import com.zamp.invoice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** JSON endpoint for submitting a human reviewer's resolutions; pending failures to resolve are read from {@code InvoiceDetailResponse.unresolvedFailures} instead of a separate endpoint. */
@RestController
@RequestMapping("/invoices/{id}")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/complete-review")
    public ResponseEntity<CompleteReviewResponse> completeReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CompleteReviewRequest request) {
        return ResponseEntity.ok(reviewService.completeReview(id, request));
    }
}
