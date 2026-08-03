package com.emme.assistant.application.port.out;

/** Outbound port for sending a text reply through the WhatsApp provider. */
public interface WhatsAppReplyPort {

  void send(String recipient, String text);
}
