package com.tabibma.review;

import com.tabibma.identity.UserContext;
import com.tabibma.review.dto.ReviewResponse;
import com.tabibma.review.dto.SubmitReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> submit(@AuthenticationPrincipal UserContext principal,
                                                  @Valid @RequestBody SubmitReviewRequest request) {
        Review review = reviewService.submit(principal, request.appointmentId(), request.rating(), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(review));
    }

    @GetMapping("/mine")
    public List<ReviewResponse> getMine(@AuthenticationPrincipal UserContext principal) {
        return reviewService.getMine(principal).stream().map(ReviewResponse::from).toList();
    }
}
