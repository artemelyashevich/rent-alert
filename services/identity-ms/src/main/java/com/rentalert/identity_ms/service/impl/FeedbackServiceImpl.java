package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.profile.FeedbackRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.identity_ms.domain.Feedback;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.mapper.FeedbackMapper;
import com.rentalert.identity_ms.repository.FeedbackRepository;
import com.rentalert.identity_ms.service.FeedbackService;
import com.rentalert.identity_ms.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserProfileService userProfileService;
    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, Long userId) {
        UserProfile userProfile = userProfileService.getById(userId);
        Feedback feedback = Feedback.builder()
            .userProfile(userProfile)
            .message(request.getMessage())
            .rating(request.getRating())
            .build();
        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback created: id={}, userId={}, rating={}", saved.getId(), userId, request.getRating());
        userProfileService.evictCachedProfile(userId);
        return feedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long feedbackId, Long userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new AppNotFoundException(InternalExceptionCode.EX404_FEEDBACK_NOT_FOUND));
        if (!feedback.getUserProfile().getId().equals(userId)) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_FEEDBACK_ACCESS_DENIED);
        }
        return feedbackMapper.toResponse(feedback);
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Long feedbackId, Long userId, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new AppNotFoundException(InternalExceptionCode.EX404_FEEDBACK_NOT_FOUND));
        if (!feedback.getUserProfile().getId().equals(userId)) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_FEEDBACK_ACCESS_DENIED);
        }
        if (request.getMessage() != null) {
            feedback.setMessage(request.getMessage());
        }
        if (request.getRating() != null) {
            feedback.setRating(request.getRating());
        }
        Feedback updated = feedbackRepository.save(feedback);
        log.info("Feedback updated: id={}, userId={}", feedbackId, userId);
        userProfileService.evictCachedProfile(userId);
        return feedbackMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long feedbackId, Long userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new AppNotFoundException(InternalExceptionCode.EX404_FEEDBACK_NOT_FOUND));
        if (!feedback.getUserProfile().getId().equals(userId)) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_FEEDBACK_ACCESS_DENIED);
        }
        feedbackRepository.delete(feedback);
        log.info("Feedback deleted: id={}, userId={}", feedbackId, userId);
        userProfileService.evictCachedProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedbacks(Pageable pageable) {
        return feedbackRepository.findAllWithUser(pageable)
            .map(feedbackMapper::toResponse);
    }
}
