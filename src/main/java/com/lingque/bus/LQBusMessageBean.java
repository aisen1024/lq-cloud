package com.lingque.bus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LQBusMessageBean {
    private String topic;
    private String msg;
}
