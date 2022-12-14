package server.api;

import commons.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import server.service.GameService;

import java.util.List;

@RestController
@RequestMapping("api/game")
public class GameController {

    private final GameService service;

    @Autowired
    public GameController(GameService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Question getById(@PathVariable("id") long id) {
        if (id < 0 || !service.existsById(id)) {
            return null;
        }
        Game g = service.getId(id);
        return g.getCurrentQuestion();
    }

    @PostMapping("/create")
    /**
     * Produces a gameID such that the player can join a game/lobby with and returns it.
     * If a lobby is already open, it will return the ID of that lobby.
     */
    public long createGame(){
        return service.createGame();
    }

    @PostMapping("create/singleplayer")
    /**
     * Produces a gameId such that the player can join a singleplayer game and returns it
     */
    public long createSingleplayerGame() {
        return service.createSingleplayerGame();
    }

    @GetMapping("/join/{id}")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    /**
     * Registers 1 player in the game.
     * @return the gameState to initially render the game screen.
     */
    public GameState joinGame(@PathVariable long id, @RequestParam String username) throws IllegalArgumentException {
        return service.joinGame(id, username);
    }
    @GetMapping("/join/singleplayer/{id}")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    /**
     * Registers the player to the singleplayer game.
     * @return the gameState to initially render the game screen.
     */
    public GameState joinSingleplayerGame(@PathVariable long id, @RequestParam String username) throws IllegalArgumentException {
        return service.joinSingleplayerGame(id, username);
    }
    @PostMapping("/init/{id}")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    /**
     * Initiates the game with the id provided.
     */
    public void initiateGame(@PathVariable long id) throws IllegalArgumentException{
        service.initiateGame(id);
    }

    /**
     * Method which returns the usernames of the players in a certain game
     * @param id the game's id
     * @return the list of usernames
     */
    @PostMapping("/init/singleplayer/{id}")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    /**
     * Initiates the singleplayer game with the id provided.
     */
    public void initiateSingpleplayerGame(@PathVariable long id) throws IllegalArgumentException{
        service.initiateSingleplayerGame(id);
    }
    @GetMapping("/players/{id}")
    public List<String> getPlayers(@PathVariable long id) {
        System.out.println(id);
        List<String> players = service.getPlayers(id);
        System.out.println("GetMapping players: " + players.size());
        return players;
    }

    /**
     * Method which returns a multiplayer game's leaderboard
     * @param id the game's id
     * @return the list of scores
     */
    @GetMapping("/leaderboard/{id}")
    public List<LeaderboardEntry> getLeaderboard(@PathVariable long id) {
        List<LeaderboardEntry> leaderboard = service.getLeaderboard(id);
        return leaderboard;
    }
    @GetMapping("/check/{id}")
    public boolean checkUsername(@PathVariable long id, @RequestParam String username) throws IllegalArgumentException {
        return service.checkUsername(id, username);
    }
}
