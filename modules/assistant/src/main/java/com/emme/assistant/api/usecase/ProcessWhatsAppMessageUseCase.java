package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;

/** Processes one verified WhatsApp message through the Assistant workflow. */
public interface ProcessWhatsAppMessageUseCase {

  /** Accepts a verified webhook and schedules durable asynchronous processing. */
  void enqueue(ProcessWhatsAppMessageCommand command);

  /** Processes one accepted webhook delivery. Intended for the durable event listener. */
  void process(ProcessWhatsAppMessageCommand command);
}
