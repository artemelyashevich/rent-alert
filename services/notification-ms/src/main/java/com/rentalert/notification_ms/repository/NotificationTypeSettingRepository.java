package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.NotificationTypeSetting;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeSettingRepository extends JpaRepository<@NonNull NotificationTypeSetting, @NonNull Long> {

}
