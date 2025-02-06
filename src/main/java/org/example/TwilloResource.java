package org.example;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.Extension;

@RestController
@Api(tags = "Twillo")
public class TwilloResource {

  @PostMapping("/incoming-call")
  public String handleIncomingCall(@RequestParam("From") String from, @RequestParam("To") String to) {
    // "From" is the caller's phone number
    System.out.println("Incoming call from: " + from);
    System.out.println("Call received at Twilio number: " + to);
    Say greating = new Say.Builder("Hi ").build();
    Say say = new Say.Builder("Connecting you to the AI assistant").build();
    Stream stream = new Stream.Builder()
            .url("wss://f09e-14-98-113-242.ngrok-free.app/media-stream")
            .build();
    Connect connect = new Connect.Builder().stream(stream).build();
    VoiceResponse response = new VoiceResponse.Builder()
            .say(say)
            .pause(new Pause.Builder().length(1).build())
            .say(new Say.Builder("We can chat now").build())
            .connect(connect).build();
    return response.toXml();
  }

  @GetMapping("/hello")
    public String get() {
        return "Twilio AI Agent";
    }
}
