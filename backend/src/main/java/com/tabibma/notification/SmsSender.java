package com.tabibma.notification;

/** Strategy interface (Architecture doc §4) — swaps SMS vendors without touching call sites. */
public interface SmsSender {

    void send(String toPhoneNumber, String message);
}
