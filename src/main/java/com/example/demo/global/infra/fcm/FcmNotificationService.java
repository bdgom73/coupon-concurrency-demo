package com.example.demo.global.infra.fcm;

import com.example.demo.domain.member.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmNotificationService {

    @Async
    public void send(Member member) {
        try {
            Thread.sleep(500);
            log.debug("FCM 알림 전송 완료 — memberId={}", member.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FCM 알림 전송 중 인터럽트 발생 — memberId={}", member.getId());
        }
    }
}
