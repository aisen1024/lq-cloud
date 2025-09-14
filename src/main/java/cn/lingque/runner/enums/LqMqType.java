package cn.lingque.runner.enums;

public enum LqMqType {
    SEQUENCE,   // 顺序队列
    LAZY,       // 延迟队列
    UNIQUE,     // 唯一队列
    UNIFIED,     // 统一队列（支持瞬时和延迟）
    STREAM_UNIFIED, // 流式统一队列（支持瞬时和延迟）
}
