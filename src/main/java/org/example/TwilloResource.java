package org.example;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Connect;
import com.twilio.twiml.voice.Pause;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.Stream;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Twillo")
public class TwilloResource {

  @PostMapping("/incoming-call")
  public String handleIncomingCall() {
    Say say = new Say.Builder("Connecting you to the AI assistant").build();

    Stream stream = new Stream.Builder()
            .url("wss://aefc-27-5-181-6.ngrok-free.app/media-stream") // Your WebSocket server URL
            .build();
    Connect connect = new Connect.Builder().stream(stream).build();
    VoiceResponse response = new VoiceResponse.Builder()
            .say(say)
            .pause(new Pause.Builder().length(1).build())
            .say(new Say.Builder("We can chat now").build()).connect(connect).build();
    return response.toXml();
  }

  @GetMapping("/hello")
    public String get() {
        return "Twilio AI Agent";
    }
}
