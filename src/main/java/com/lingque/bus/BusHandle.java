package com.lingque.bus;

public interface BusHandle<E> {
    public void handle(E event);
}
