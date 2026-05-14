package konkuk.clog.domain.chat.domain;

/**
 * MongoDB 채팅 세션 상태 — 현재는 활성만 사용(요약 후 이전 세션·메시지는 삭제됨).
 */
public enum ChatSessionStatus {
    ACTIVE
}
