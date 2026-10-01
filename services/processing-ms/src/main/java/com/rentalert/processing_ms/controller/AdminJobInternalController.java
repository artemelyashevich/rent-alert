package com.rentalert.processing_ms.controller;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.scheduler.CurrencyRateScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/jobs")
@RequiredArgsConstructor
public class AdminJobInternalController {

    private final ObjectProvider<CurrencyRateScheduler> currencyRateScheduler;

    @PostMapping("/currency-rates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncCurrencyRates() {
        CurrencyRateScheduler scheduler = currencyRateScheduler.getIfAvailable();
        if (scheduler == null) {
            throw new AppException(InternalExceptionCode.EX503_JOB_DISABLED, HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!scheduler.run(JobInitiator.ADMIN)) {
            throw new AppException(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING, HttpStatus.CONFLICT);
        }
    }
}
