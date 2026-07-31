package com.tabibma.notification;

/** Strategy interface (Architecture doc §4) — swaps email vendors without touching call sites. */
public interface EmailSender {

    void send(String toEmail, String subject, String body);
}
