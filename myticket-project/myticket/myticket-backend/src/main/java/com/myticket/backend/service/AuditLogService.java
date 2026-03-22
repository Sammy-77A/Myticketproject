package com.myticket.backend.service;

import com.myticket.backend.model.AuditLog;
import com.myticket.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(Long actorId, String actorEmail, String action, String entityType, Long entityId, String detail) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail)
                .build();
        
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logAction(String actorEmail, String action, String detail) {
        log(null, actorEmail, action, null, null, detail);
    }
}

