package com.swift.message.process.domain.exception;

/** 报文为空、格式非法或缺少目标报文层时抛出。 */
public class MessageParseException extends IllegalArgumentException {
    public MessageParseException(String message) {
        super(message);
    }

    public MessageParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
