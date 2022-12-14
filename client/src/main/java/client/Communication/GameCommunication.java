package client.Communication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Inject;
import commons.GameState;
import commons.LeaderboardEntry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class GameCommunication {
    private HttpClient client;

    private static Gson gson = new Gson();

    @Inject
    public GameCommunication(HttpClient client) {
        this.client = client;
    }

    /**
     * Sends an HTTP request to the server to create a game, gets the ID of the game.
     *
     * @return the ID of the game state that is created, if an exception occurs, return -1;
     */
    public long createGame(String serverString) {
        HttpRequest request = null;
        try
        {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(serverString + "/api/game/create"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
        }
        catch(IllegalArgumentException exc){
            return -1;
        }
        try {
            return Long.parseLong(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Sends an HTTP request to the server to create a singleplayer game, gets the ID of the game.
     *
     * @return the ID of the game state that is created, if an exception occurs, return -1;
     */
    public long createSingleplayerGame() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/game/create/singleplayer"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            return Long.parseLong(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Sends the server the gameID that the user wants to join, together with the username they have chosen.
     *
     * @param gameId   the ID of the game, the user wants to join. Usually, this would not
     *                 necessary, since there is always only 1 game that can be joined at a time, but it is designed that way.
     * @param username The username that the client has chosen to join the game with.
     * @return the gameState to initially render the game with, returns null if an exception occurred.
     */
    public GameState joinGame(long gameId, String username, String serverString) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/join/" + gameId + "?username=" + username))
                .GET()
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final ObjectMapper mapper = new ObjectMapper();
            final TypeReference<GameState> typeRef = new TypeReference<>() {
            };
            GameState gameState = mapper.readValue(response.body(), typeRef);
            System.out.println("Leaderboard: " + gameState.leaderboard.size());

            return gameState;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends the server the singleplayer game's ID that the user wants to join, together with the username they have chosen.
     *
     * @param gameId   the ID of the singleplayer game, the user wants to join
     * @param username The username that the client has chosen to join the singleplayer game with.
     * @return the gameState to initially render the game with, returns null if an exception occurred.
     */
    public GameState joinSingleplayerGame(long gameId, String username, String serverString) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/join/singleplayer/" + gameId + "?username=" + username))
                .GET()
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final ObjectMapper mapper = new ObjectMapper();
            final TypeReference<GameState> typeRef = new TypeReference<>() {
            };
            GameState gameState = mapper.readValue(response.body(), typeRef);
            System.out.println("Leaderboard: " + gameState.leaderboard.size());

            return gameState;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends a request to the server to initiate the game with id gameId.
     *
     * @param gameId The id of the game to be initiated.
     */
    public void initiateGame(long gameId, String serverString) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/init/" + gameId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a request to the server to initiate the singleplayer game with id gameId.
     *
     * @param gameId The id of the game to be initiated.
     */
    public void initiateSingleplayerGame(long gameId, String serverString) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/init/singleplayer/" + gameId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method which gets the list of players' usernames from the game with ID gameId
     *
     * @param gameId the game's ID
     * @return a list of usernames
     */
    public List<String> getPlayers(long gameId, String serverString) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/players/" + gameId))
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final ObjectMapper mapper = new ObjectMapper();
        final TypeReference<List<String>> typeRef = new TypeReference<>() {
        };
        return mapper.readValue(response.body(), typeRef);
    }

    /**
     * Method which checks whether the username has already been taken in a certain game
     *
     * @param gameId   the game's ID
     * @param username the username which needs to be checked
     * @return true, if the username has not been taken and can be used, false otherwise
     */
    public boolean checkUsername(long gameId, String username, String serverString) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverString + "/api/game/check/" + gameId + "?username=" + username))
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final ObjectMapper mapper = new ObjectMapper();
        final TypeReference<Boolean> typeRef = new TypeReference<>() {
        };
        boolean answer = mapper.readValue(response.body(), typeRef);
        return answer;
    }

    /**
     * Method which returns the leaderboard for a multiplayer game
     *
     * @param gameId the game's id
     * @return the list of leaderboard entries
     */
    public List<LeaderboardEntry> getLeaderboardMultiplayer(long gameId, String server) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(server+"/api/game/leaderboard/" + gameId))
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final ObjectMapper mapper = new ObjectMapper();
        final TypeReference<List<LeaderboardEntry>> typeRef = new TypeReference<>() {
        };
        return mapper.readValue(response.body(), typeRef);
    }

    /**
     * Method which returns the leaderboard for a singleplayer game. Since this method returns the global
     * leaderboard, it doesn't need the game's id
     * @return the list of leaderboard entries
     */
    public List<LeaderboardEntry> getLeaderboardSingleplayer () throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/leaderboard/"))
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final ObjectMapper mapper = new ObjectMapper();
        final TypeReference<List<LeaderboardEntry>> typeRef = new TypeReference<>() {
        };
        return mapper.readValue(response.body(), typeRef);
    }

    /**
         * Method which returns the leaderboard containing the top 10 entries on the specified server
         * @return the list of leaderboard entries
         */
        public List<LeaderboardEntry> getTopLeaderboard(String server) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server+"/api/leaderboard/"))
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final ObjectMapper mapper = new ObjectMapper();
            final TypeReference<List<LeaderboardEntry>> typeRef = new TypeReference<>() {
            };
            return mapper.readValue(response.body(), typeRef);
        }

    /**
     * Method to add a leaderboard entry to the database on a specific server
     * @param server
     * @param entry
     * @throws IOException
     * @throws InterruptedException
     */

    public void addEntry(String server, LeaderboardEntry entry) throws IOException, InterruptedException{

            String requestBody = (gson.toJson(entry));

        System.out.println(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server+"/api/leaderboard/"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
            }catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }