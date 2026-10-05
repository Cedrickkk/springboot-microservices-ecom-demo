package com.microservices.ecom.repository;

import com.microservices.ecom.domain.Notification;
import com.microservices.ecom.domain.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findAllByTypeOrderByNotificationDateDesc(NotificationType type);
}
