package com.rentalert.identity_ms.service;

import com.rentalert.dto.profile.FeedbackRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {

    FeedbackResponse createFeedback(FeedbackRequest request, Long userId);

    FeedbackResponse getFeedbackById(Long feedbackId, Long userId);

    FeedbackResponse updateFeedback(Long feedbackId, Long userId, FeedbackRequest request);

    void deleteFeedback(Long feedbackId, Long userId);

    Page<FeedbackResponse> getAllFeedbacks(Pageable pageable);
}
