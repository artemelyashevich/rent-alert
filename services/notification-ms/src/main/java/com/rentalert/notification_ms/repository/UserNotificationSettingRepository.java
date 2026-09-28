package com.rentalert.notification_ms.repository;

import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    List<UserNotificationSetting> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserNotificationSetting s where s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    Optional<UserNotificationSetting> findByUserIdAndEngine(Long userId, NotificationEngine engine);
}
