package server.service;

import commons.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {

    static class GameRepository {
        static long idGenerator = 0;
        private Map<Long, Game> games;

        GameRepository() {
            games = new HashMap<Long, Game>();
        }

        /**
         * Gets the game with the according ID
         * @param id the ID of the game
         * @return
         * @throws IllegalArgumentException
         */
        Game getId(long id) throws IllegalArgumentException {
            if (!existsById(id)) throw new IllegalArgumentException();
            return games.get(id);
        }

        void save(Game game) {
            Objects.requireNonNull(game, "game cannot be null");
            game.id = idGenerator++;
            games.put(game.id, game);
        }

        boolean existsById(long id) {
            return games.containsKey(id);
        }
    }

    static class PlayerRepository {
        static long idGenerator;
        private Map<Long, Player> players;

        PlayerRepository() {
            players = new HashMap<>();
        }

        Player getId(long id) throws IllegalArgumentException {
            if (!existsById(id)) throw new IllegalArgumentException();
            return players.get(id);
        }

        void save(Player player) {
            Objects.requireNonNull(player, "player cannot be null");
            player.id = idGenerator++;
            players.put(player.id, player);
        }

        boolean existsById(long id) {
            return players.containsKey(id);
        }
        Map<Long, Player> getPlayers() {
            return  players;
        }
    }

    //currentGame is the game where new players will join. Basically the lobby
    private Game currentGame;

    int stateInteger = -1;//0 if state is QUESTION, 1 if state is INTERVAL

    private final GameRepository gameRepository = new GameRepository();
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final QuestionService questionService;
    private final LongPollingService longPollingService;
    private long timeOfSent;

    @Autowired
    public GameService(QuestionService questionService, LongPollingService longPollingService) {
        //make timeOfSent = -1 or 0 if awarding the proper amount of points is buggy
        this.questionService = questionService;
        this.longPollingService = longPollingService;
        createCurrentGame();
    }

    /**
     * A method which creates the current game - which is the lobby
     */
    private void createCurrentGame() {
        List<Question> questions = new ArrayList<>();
        questions = questionService.getAll();
        currentGame = new Game(questions);
        gameRepository.save(currentGame);
    }

    public Game getId(long id) {
        return gameRepository.getId(id);
    }

    /**
     * Produce a gameId which the player client can join a game or its lobby with
     * This method can be used even if there is only one lobby that can be joined at a given moment. Then it would
     * return the gameId of the game that the player is wanted to join.
     * @return the gameId
     */
    public long createGame() {
        //TODO: Example questions till question import is fixed
        //List<Question> questions = questionService.getAll();

        //List<Question> questions = new ArrayList<>();
        //questions = questionService.getAll();
        //Game g = new Game(questions);
        //gameRepository.save(g);
        return currentGame.id;
    }

    /**
     * Register *one player* into a game with the given username. This will fail if the lobby of the game already
     * has a player with the given username.
     *
     * @param gameId the id of the game to join
     * @param username the username that the player wants to join with
     * @return the GameState to initially render the lobby or game screen
     * @throws IllegalArgumentException if the gameId is invalid.
     */
    public GameState joinGame(long gameId, String username) throws IllegalArgumentException {
        System.out.println("Username for player: " + username);
        Player player = new Player(username, 0);
        Game game = gameRepository.getId(gameId);
        GameState state = game.getState(player);
        if (!game.addPlayer(player)) {
            state.isError = true;
            state.message = "usernameAlreadyInGame";
            System.out.println("Username already taken");
            return state;
        }
        state = game.getState();
        for (Player otherPlayer : game.players) {
            state.setPlayer(otherPlayer);
            sendToPlayer(otherPlayer.id, state);
        }
        playerRepository.save(player);
        return state;
    }

    /**Initiate a game. Do not allow other players to join and show the first question.
     * At the same time, it also creates a new current game (a new lobby)
     *
     * @param gameId the id of the game to initiate
     * @throws IllegalArgumentException if the gameId is invalid.
     */
    public void initiateGame(long gameId) throws IllegalArgumentException {
        Game game = gameRepository.getId(gameId);

        if (game.started) return;
        game.started = true;
        game.stage = GameState.Stage.QUESTION;
        questionPhase(game);
        createCurrentGame();
    }

    public List<String> getPlayers(long id) {
        Map<Long, Player> players = playerRepository.getPlayers();
        List<String> playersForGame = new ArrayList<>();
        for(Long playerId : players.keySet()) {
            Player player = players.get(playerId);
            if(player.gameId == id) {
                playersForGame.add(player.username);
            }
        }
        System.out.println("Players in the list: " + playersForGame.size());
        System.out.println("Player in the repository: " + players.entrySet().size());
        return playersForGame;
    }

    //methods that call each other back and forth to have a single game running.
    public void questionPhase(final Game game) {
        stateInteger = 0;

        game.stage = GameState.Stage.QUESTION;
        GameState state = game.getState();

        for (Player player : game.players) {
            state.setPlayer(player);
            state.stage = GameState.Stage.QUESTION;
            sendToPlayer(player.id, state);
        }

        //switch to interval phase 30 seconds later so we make sure all player timers have run out.
        //TODO: have a flexible delay in case everyone's timer is shortened.
        new Thread(() -> {
            try {
                Thread.sleep(10_000);
                intervalPhase(game);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void intervalPhase(final Game game) {
        stateInteger = 1;

        game.stage = GameState.Stage.INTERVAL;
        GameState state = game.getState();

        for (Player player : game.players) {
            state.setPlayer(player);
            state.setPlayerAnswer(player.answer);
            sendToPlayer(player.id, state);
        }
        score(game);
        //switch to question phase 5 seconds later so the player has time to see the correctness of their answer
        if (game.progressGame()) {
            new Thread(() -> {
                try {
                    Thread.sleep(5_000);
                    questionPhase(game);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public boolean existsById(long id) {
        return gameRepository.existsById(id);
    }

    /**Send the game state to a player.
     *
     * Note that the connection is only available after the player makes a request to
     * /api/listen, therefore you can't just send new data to a player without some
     * delay in between!
     *
     * @param playerId
     * @param state
     */
    public void sendToPlayer(Long playerId, GameState state) {
        longPollingService.sendToPlayer(playerId, state);
        timeOfSent = System.nanoTime();
    }

    public void submitByPlayer(Long playerId, String ans, Long gameId) {
        if(stateInteger==1){
            return;
        }
        Game g = gameRepository.getId(gameId);
        Player p;
        int pos = 0;
        while(pos<g.players.size() && g.players.get(pos).id != playerId){
            pos++;
        }
        p = g.players.get(pos);
        if(p.answer == null || p.answer.isEmpty()&&stateInteger==0) {
            p.answer = ans;
            p.timeToAnswer = (System.nanoTime() - timeOfSent)/1000000000;//time it took user to answer in seconds
            System.out.println("it took user " + p.timeToAnswer + " seconds to answer");//debug
        }
    }

    public void score(Game g) {
        System.out.println("Scores:");
        Question q = g.questions.get(g.currentQuestion);
        for(Player p: g.players){
            if(p.answer!=null && p.answer.equals(q.answer)) {
                System.out.println("answer recieved");//debug
                p.score = (long) (p.score + (10.0 - p.timeToAnswer)*10);
            }
            System.out.println("this is quote after answer recieved");//debug
            p.answer = null;
            System.out.println(p.id + ": " + p.score);

        }
    }
}
