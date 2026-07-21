package com.swift.message.process;

/** 构造报文时字段缺失或内容不符合 SDK 构造规则时抛出。 */
public class MessageBuildException extends IllegalArgumentException {
    public MessageBuildException(String message) {
        super(message);
    }

    public MessageBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
