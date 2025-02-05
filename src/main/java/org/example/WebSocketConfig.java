package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

  private final OpenAiTwilloMiddleLayerResource openAiTwilloMiddleLayerResource;

  @Autowired
  public WebSocketConfig(OpenAiTwilloMiddleLayerResource openAiTwilloMiddleLayerResource) {
    this.openAiTwilloMiddleLayerResource = openAiTwilloMiddleLayerResource;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
    log.info("Registering WebSocket handlers");
    webSocketHandlerRegistry.addHandler(openAiTwilloMiddleLayerResource, "/media-stream")
            .setAllowedOrigins("*");
  }
}