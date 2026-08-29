package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiNailDesignExtractor;
import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opt-in Spring AI composition root for structured quote extraction. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai.spring-extraction", name = "enabled", havingValue = "true")
public class SpringAiQuoteExtractionConfiguration {

  @Bean(name = "ollamaChatModel")
  @ConditionalOnMissingBean(name = "ollamaChatModel")
  ChatModel ollamaChatModel(AiProperties aiProperties) {
    return OllamaChatModel.builder()
        .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.chat().baseUrl()).build())
        .options(OllamaChatOptions.builder().model(aiProperties.chat().model()).build())
        .build();
  }

  @Bean(name = "aiQuoteExtractionChatClient")
  @ConditionalOnMissingBean(name = "aiQuoteExtractionChatClient")
  ChatClient quoteExtractionChatClient(ChatModel chatModel) {
    return ChatClient.create(chatModel);
  }

  @Bean
  @ConditionalOnMissingBean(NailDesignExtractor.class)
  NailDesignExtractor nailDesignExtractor(
      ChatClient aiQuoteExtractionChatClient,
      SpringAiExtractionProperties properties,
      ObjectProvider<DesignImageReader> imageReaders,
      AiTraceRecorder traceRecorder,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      AiExecutorProperties executorProperties) {
    DesignImageReader imageReader = imageReaders.getIfAvailable(() -> key -> Optional.empty());
    if (modelExecutionScheduler.isPresent()) {
      return new SpringAiNailDesignExtractor(
          aiQuoteExtractionChatClient,
          properties.modelVersion(),
          properties.promptVersion(),
          properties.schemaVersion(),
          imageReader,
          traceRecorder,
          modelExecutionScheduler.orElseThrow(),
          executorProperties.modelAdmissionTimeout());
    }
    return new SpringAiNailDesignExtractor(
        aiQuoteExtractionChatClient,
        properties.modelVersion(),
        properties.promptVersion(),
        properties.schemaVersion(),
        imageReader,
        traceRecorder);
  }

  NailDesignExtractor nailDesignExtractor(
      ChatClient aiQuoteExtractionChatClient,
      SpringAiExtractionProperties properties,
      ObjectProvider<DesignImageReader> imageReaders,
      AiTraceRecorder traceRecorder) {
    DesignImageReader imageReader = imageReaders.getIfAvailable(() -> key -> Optional.empty());
    return new SpringAiNailDesignExtractor(
        aiQuoteExtractionChatClient,
        properties.modelVersion(),
        properties.promptVersion(),
        properties.schemaVersion(),
        imageReader,
        traceRecorder);
  }

  NailDesignExtractor nailDesignExtractor(
      ChatClient aiQuoteExtractionChatClient,
      SpringAiExtractionProperties properties,
      ObjectProvider<DesignImageReader> imageReaders) {
    return nailDesignExtractor(
        aiQuoteExtractionChatClient, properties, imageReaders, NoopAiTraceRecorder.INSTANCE);
  }
}
