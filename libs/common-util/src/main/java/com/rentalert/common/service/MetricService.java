package com.rentalert.common.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetricService {

    private final MeterRegistry meterRegistry;

    public void increment(String metricName, Tag... tags) {
        Counter.builder(metricName)
            .tags(Tags.of(tags))
            .register(meterRegistry)
            .increment();
    }

    public void increment(String metricName, double amount, Tag... tags) {
        Counter.builder(metricName)
            .tags(Tags.of(tags))
            .register(meterRegistry)
            .increment(amount);
    }

    public <T> T recordTime(
        String metricName,
        Supplier<T> supplier,
        Tag... tags
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = supplier.get();
            stopTimer(sample, metricName, "success", tags);
            return result;
        } catch (Exception ex) {
            stopTimer(sample, metricName, "error", tags);
            throw ex;
        }
    }

    public void recordTime(
        String metricName,
        Runnable runnable,
        Tag... tags
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            runnable.run();
            stopTimer(sample, metricName, "success", tags);
        } catch (Exception ex) {
            stopTimer(sample, metricName, "error", tags);
            throw ex;
        }
    }

    public <T extends Number> void gauge(
        String metricName,
        T number,
        Tag... tags
    ) {
        Gauge.builder(metricName, number, Number::doubleValue)
            .tags(Tags.of(tags))
            .register(meterRegistry);
    }

    public LongTaskTimer.Sample startLongTask(
        String metricName,
        Tag... tags
    ) {
        LongTaskTimer timer = LongTaskTimer.builder(metricName)
            .tags(Tags.of(tags))
            .register(meterRegistry);
        return timer.start();
    }

    public void stopLongTask(LongTaskTimer.Sample sample) {
        sample.stop();
    }

    public void record(
        String metricName,
        long duration,
        TimeUnit unit,
        Tag... tags
    ) {
        Timer.builder(metricName)
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95, 0.99)
            .tags(Tags.of(tags))
            .register(meterRegistry)
            .record(duration, unit);
    }

    private void stopTimer(
        Timer.Sample sample,
        String metricName,
        String status,
        Tag... tags
    ) {
        Tags allTags = Tags.of(tags).and("status", status);
        sample.stop(
            Timer.builder(metricName)
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .tags(allTags)
                .register(meterRegistry)
        );
    }

    public Tag tag(String key, String value) {
        return Tag.of(key, value);
    }

    public void recordCpuUsage(String metricName, Tag... tags) {
        double load = currentProcessCpuLoadPercent();
        if (load < 0) {
            return;
        }
        DistributionSummary.builder(metricName)
            .tags(Tags.of(tags))
            .register(meterRegistry)
            .record(load);
    }

    private double currentProcessCpuLoadPercent() {
        if (ManagementFactory.getOperatingSystemMXBean()
            instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            return sunOsBean.getProcessCpuLoad() * 100;
        }
        return -1;
    }
}
