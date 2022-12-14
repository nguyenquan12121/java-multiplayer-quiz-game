package server.api;

import commons.GameState;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import server.service.LongPollingService;

import java.util.List;

@RestController
@RequestMapping("/api/listen")
public class ListenController {
    LongPollingService service;

    public ListenController(LongPollingService service) {
        this.service = service;
    }

    /**Sends the player back a DeferredResult. This is how long polling is implemented in Spring. The DeferredResult
     * is passed to the GameService which may setResult it to send any necessary updates to the player. After receiving
     * the response this way, the player client should immediately make another request here so the future updates
     * can also be sent to them.
     * @param playerId the playerId received as a query parameter in the form "/api/listen?playerId=0"
     * @return the DeferredResult so Spring can send a response later
     * @throws IllegalArgumentException when the request lacks the necessary playerId parameter
     */
    @GetMapping("")
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public DeferredResult<List<GameState>> getListen(@RequestParam Long playerId) throws IllegalArgumentException {
        if (playerId == null) throw new IllegalArgumentException();
        DeferredResult<List<GameState>> result = new DeferredResult<>(30000L);
        service.registerPlayerConnection(playerId, result);
        System.out.println(result.getResult());
        return result;
    }
}
