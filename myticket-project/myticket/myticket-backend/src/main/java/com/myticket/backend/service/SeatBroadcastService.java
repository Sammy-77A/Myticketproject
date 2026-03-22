package com.myticket.backend.service;

import com.myticket.common.dto.AttendanceUpdateMessage;
import com.myticket.common.dto.SeatUpdateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeatBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public SeatBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastSeatUpdate(Long eventId, Long tierId, int remainingSeats, int totalCapacity) {
        SeatUpdateMessage msg = new SeatUpdateMessage(eventId, tierId, remainingSeats, totalCapacity);
        messagingTemplate.convertAndSend("/topic/seats/" + eventId, msg);
    }

    public void broadcastAttendanceUpdate(Long eventId, int checkedIn, int totalCapacity, String lastScannedName, String lastScannedTier) {
        AttendanceUpdateMessage msg = new AttendanceUpdateMessage(eventId, checkedIn, totalCapacity, lastScannedName, lastScannedTier);
        messagingTemplate.convertAndSend("/topic/attendance/" + eventId, msg);
    }
}
