package com.lingque.bus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BusHandleBeanInfo {
    private Class entityClass;
    private BusHandle busHandle;
}
