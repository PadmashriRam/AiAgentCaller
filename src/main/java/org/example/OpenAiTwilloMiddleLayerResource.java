package org.example;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
@Slf4j
public class OpenAiTwilloMiddleLayerResource extends BinaryWebSocketHandler {
  private WebSocketClient openAiWebSocket;
  private WebSocketSession twilioSession;
  private String streamSid;
  private static final String TYPE = "session.update";
  private static final String TURN_DETECTION_TYPE = "server_vad";
  private static final String INPUT_AUDIO_FORMAT = "g711_ulaw";
  private static final String OUTPUT_AUDIO_FORMAT = "g711_ulaw";
  private static final String VOICE = "alloy";
  private static final String INSTRUCTIONS = "**Business Goal**\n- Your task is to act as 'YourShoppingPartner Support Bot' Voice Assistant and respond to queries ONLY related to e-commerce shopping domain related topics.  Always considering our business context while also engaging in small talk. Do not use other tools, internet or training data to respond on these topics. Your response MUST intake based on internal policies.\n\n**Acceptable Topics**\n- E-Commerce online Shopping Application such as 'Order management', 'Product Detail', 'Track, Place, Return, and or Modify Order', 'Shipping Charges to ship a product', 'Gift Card', 'Balance in Brand Card', 'Running Offers', 'Account Prime Service subscription', or any e-commerce shopping related query.\n\n**Response Guidelines**\n- Do not entrained query outside of Acceptable Topic scope. For non-acceptable topics, kindly redirect users saying that you are not train for it, maintaining professionalism and correctness.\n- Do not use other tools or knowledge base or trained data to respond on Acceptable Topics. Generic response is not your goal. ALWAYS call `get_user_query` method. Wait for the [function|tool] response before reply.\n- If the query is not related to Small-Talk but is an acceptable topic, calling `get_user_query` function. Wait for response.\n- Dont respond without getting response from function. You can engage customer saying that we are still working to get your query response if function calling is taking more than 10 seconds. Sorry for delay!\n\nKeep the customer engaged with friendly and conversational tone while waiting for a function or tool response. Ensure there are no long pauses or silent periods.\n\n- Use phrases to reassure and engage the customer.\n- Express empathy for any issues the customer is facing.\n- Proactively communicate when delays occur.\n\n# Examples\n\n**Example 1:**\n\n- User: \"Can you find me a good deal?\"\n- Assistant: \"Sure, let me find the best option for you!\"\n- User: \"Thanks, how long might it take?\"\n- Assistant: \"It's taking a little longer than usual. I'm on it!\"\n\n**Example 2:**\n\n- User: \"I need help with a return.\"\n- Assistant: \"I'm sorry to hear that. I'll find the best possible option for you.\"\n- User: \"Thanks, I appreciate it.\"\n- Assistant: \"Hang on, I'm working on it!\"\n\n**Interaction Protocol**\n- Use minimal back-and-forth phrasing with a focus on customer satisfaction.\n- Handle small talk and Q&A adeptly even during business interactions.\n- For multiple questions in a single query, address supported topics and politely explain why others can't be entertained.\n- IDENTIFY language of user's latest message. ENSURE response in the same language clearly and empathetically.\n";
  private static final String MODALITIES = "[\"text\", \"audio\"]";
  private static final double TEMPERATURE = 0.8;
  private final RestTemplate restTemplate;

  private static final String TOOLS = "["
          + "{"
          +     "\"type\": \"function\","
          +     "\"name\": \"get_weather\","
          +     "\"description\": \"Get current weather for a specified city\","
          +     "\"parameters\": {"
          +         "\"type\": \"object\","
          +         "\"properties\": {"
          +             "\"city\": {"
          +                 "\"type\": \"string\","
          +                 "\"description\": \"The name of the city for which to fetch the weather.\""
          +             "}"
          +         "},"
          +         "\"required\": [\"city\"]"
          +     "}"
          + "}"
          + "]";

  public OpenAiTwilloMiddleLayerResource(RestTemplate restTemplate) {
    super();
    this.restTemplate = restTemplate;
    log.info("OpenAiTwilloMiddleLayerResource created");
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    log.info("Twilio client connected: " + session.getId());

    // Connect to OpenAI WebSocket
    connectToOpenAi(session);
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    if (openAiWebSocket != null && openAiWebSocket.isOpen()) {
      log.info("in handleBinaryMessage method");
      openAiWebSocket.send(message.getPayload().array());
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    JSONObject data = new JSONObject(message.getPayload());

    if (data.getString("event").equals("media") && openAiWebSocket.isOpen()) {
      JSONObject audioAppend = new JSONObject();
      audioAppend.put("type", "input_audio_buffer.append");
      audioAppend.put("audio", data.getJSONObject("media").getString("payload"));
      openAiWebSocket.send(audioAppend.toString());
    } else if (data.getString("event").equals("start")) {
      streamSid = data.getJSONObject("start").getString("streamSid");
      log.info("Incoming stream has started: " + streamSid);
    } else if (data.getString("event").equals("stop")) {
      log.info("Incoming stream has stopped");
    } else {
      log.info("Received unknown event from Twilio: " + data);
    }
  }

  private void connectToOpenAi(WebSocketSession twilioSession) {
    Map<String, String> headers = new HashMap<>();
    headers.put("api-key", "889cb303b8f843e6b051d9f65ae2f63a");
    openAiWebSocket = new WebSocketClient(URI.create("wss://freshcaller-swedencentral-ai-stage01.openai.azure.com/openai/realtime?api-version=2024-10-01-preview&deployment=gpt-4o-realtime-preview"), headers) {
      @Override
      public void onOpen(ServerHandshake handshake) {
        log.info("Connected to OpenAI WebSocket");
        // You might want more initialization here
        // Example: sending session update
        sendSessionUpdate();
      }

      @Override
      public void onMessage(String message) {
        try {
          JSONObject response = new JSONObject(message);
          handleOpenAiResponse(response);
        } catch (Exception e) {
          log.info("Error processing message from OpenAI: " + e.getMessage());
        }
      }

      @Override
      public void onMessage(ByteBuffer bytes) {
        // Forward any messages from OpenAI to Twilio
        try {
          twilioSession.sendMessage(new BinaryMessage(bytes));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onClose(int code, String reason, boolean remote) {
        log.info("OpenAI WebSocket closed: " + reason);
      }

      @Override
      public void onError(Exception ex) {
        log.info("OpenAI WebSocket error: " + ex.getMessage());
      }

      private void handleOpenAiResponse(JSONObject response) {
        // Check if the event type is one of the ones we are interested in
        String eventType = response.getString("type");
        log.info("Received event from OpenAI: " + eventType);

        try {
          if ("session.updated".equals(eventType)) {
            log.info("Session updated successfully: " + response);
          }

          if ("input_audio_buffer.speech_started".equals(eventType)) {
            // If Twilio session is still open, send a clear event
            JSONObject clearEvent = new JSONObject();
            clearEvent.put("event", "clear");
            clearEvent.put("streamSid", streamSid);
            sendMessageToTwilio(clearEvent.toString());
          }

          if ("response.audio.delta".equals(eventType) && response.has("delta")) {
            log.info("Received audio delta from OpenAI: " + response);
            // Handle sending audio data back to Twilio
            String deltaEncoded = response.getString("delta");
            byte[] audioPayload = Base64.getDecoder().decode(deltaEncoded);

            JSONObject audioDelta = new JSONObject();
            audioDelta.put("event", "media");
            audioDelta.put("streamSid", streamSid);
            audioDelta.put("media", new JSONObject().put("payload", Base64.getEncoder().encodeToString(audioPayload)));

            sendMessageToTwilio(audioDelta.toString());
          }

          if ("response.function_call_arguments.done".equals(eventType)) {
            log.info("Function call arguments received: " + response);
            try {
              Map<String, String> data = JsonUtils.getMapFromJson(response.toString());
              Map<String, String> args = JsonUtils.getMapFromJson(data.get("arguments"));
              log.info("Function call arguments: " + args);
              String callId = data.get("call_id").toString();
              sendFunctionCallResult(callVoiceApi("get_user_query", data.get("arguments")), callId);
            } catch (Exception e) {
              log.error("Error parsing function call arguments: {}", e.getMessage());
            }
          }

          if ("error".equals(eventType)) {
            log.info("Error from OpenAI: " + response);
          }
        } catch (Exception e) {
          log.info("Error during message handling: " + e);
        }
      }

      public String callVoiceApi(String getUserQuery, String args) {
        String url = "https://api.qa1freshbots.com/api/slack/voicecall";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> twilio = new HashMap<>();
        twilio.put("function_name", getUserQuery);
        twilio.put("arguments", args);

        Map<String, Object> request = new HashMap<>();
        request.put("twilio", twilio);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getBody() != null) {
          Map<String, Object> responseBody = (Map<String, Object>) response.getBody().get("body");
          if (responseBody != null) {
            return (String) responseBody.get("assistantResponse");
          }
        }
        return "No response from API";
      }

      private void sendMessageToTwilio(String message) {
        if (twilioSession.isOpen()) {
          try {
            twilioSession.sendMessage(new TextMessage(message));
          } catch (Exception e) {
            log.info("Error sending message to Twilio: " + e.getMessage());
          }
        }
      }
      public void sendFunctionCallResult(String result, String callId) {
        log.info("Sending function call result to OpenAI: " + result);
        JSONObject resultJson = new JSONObject();
        resultJson.put("type", "conversation.item.create");
        JSONObject itemJson = new JSONObject();
        itemJson.put("type", "function_call_output");
        itemJson.put("output", result);
        itemJson.put("call_id", callId);
        resultJson.put("item", itemJson);
        openAiWebSocket.send(resultJson.toString());

        JSONObject rpJson = new JSONObject();
        rpJson.put("type", "response.create");
        log.info("Sending response create to OpenAI: " + rpJson);
        openAiWebSocket.send(rpJson.toString());
      }
    };

    CompletableFuture.runAsync(() -> {
      try {
        openAiWebSocket.connectBlocking();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
  }

  private void sendSessionUpdate() {
      String instructionsEscaped = JSONObject.quote(INSTRUCTIONS);
      String sessionUpdateJson = String.format(
              "{"
                      + "\"type\": \"%s\","
                      + "\"session\": {"
                      + "\"instructions\": %s,"
                      + "\"turn_detection\": %s,"
                      + "\"input_audio_format\": \"%s\","
                      + "\"output_audio_format\": \"%s\","
                      + "\"voice\": \"%s\","
                      + "\"temperature\": %s,"
                      + "\"max_response_output_tokens\": 4096,"
                      + "\"modalities\": %s,"
                      + "\"input_audio_transcription\": {"
                      + "\"model\": \"whisper-1\""
                      + "},"
                      + "\"tool_choice\": \"auto\","
                      + "\"tools\": %s"
                      + "}"
                      + "}",
              TYPE,
              instructionsEscaped,
              getTurnDetectionConfig(),
              INPUT_AUDIO_FORMAT,
              OUTPUT_AUDIO_FORMAT,
              VOICE,
              TEMPERATURE,
              MODALITIES,
              TOOLS
      );
      log.info("Sending session update to OpenAI: " + sessionUpdateJson);
      openAiWebSocket.send(sessionUpdateJson);
  }

  private String getTurnDetectionConfig() {
    return String.format(
            "{"
                    + "\"type\": \"%s\","
                    + "\"threshold\": %s,"
                    + "\"prefix_padding_ms\": %s,"
                    + "\"silence_duration_ms\": %s"
                    + "}",
            "server_vad", 0.5, 300, 500
    );
  }
}