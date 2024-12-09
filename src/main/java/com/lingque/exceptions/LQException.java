package com.lingque.exceptions;

import lombok.Data;

/**
 * @author aisen
 * @date 2024/9/27
 * @desc 简单说一下
 **/
@Data
public class LQException extends RuntimeException{
    private String message;

    public LQException(String message1) {
        super(message1);
        this.message = message1;
    }
}
