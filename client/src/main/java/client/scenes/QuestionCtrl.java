package client.scenes;

import client.Communication.AnswerCommunication;
import client.Communication.ImageCommunication;
import client.Communication.PowerUpsCommunication;
import commons.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static javafx.beans.binding.Bindings.createObjectBinding;

public class QuestionCtrl {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button answer1;

    @FXML
    private Button answer2;

    @FXML
    private Button answer3;

    private boolean[] selectedCSS = {false, false, false};

    @FXML
    private Button emoteAngry;

    @FXML
    private Button emoteLOL;

    @FXML
    private Button emoteSweat;

    @FXML
    private Button emoteClap;

    @FXML
    private Button emoteWin;

    @FXML
    private Button doublePoints;

    @FXML
    private Button eliminateWrongAnswer;

    @FXML
    private Button halfTime;

    @FXML
    private AnchorPane playerPosition;

    @FXML
    private Label playerScore;

    @FXML
    private Label playerUsername;

    @FXML
    private Label playerRank;

    @FXML
    private ImageView questionImage;

    @FXML
    private Label questionText;

    @FXML
    private Label questionTitle;

    @FXML
    private Label questionTime;

    @FXML
    private SplitPane allLeaderboard;

    @FXML
    private TableView<LeaderboardEntry> leaderboard;

    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardUsernames;
    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardRanks;
    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardScores;

    @FXML
    private TableView<EmoteEntry> emotes;

    @FXML
    private TableColumn<EmoteEntry, String> emotesUsernameColumn;

    @FXML
    private TableColumn<EmoteEntry, ImageView> emotesEmoteColumn;

    @FXML
    private AnchorPane root;

    @FXML
    private TextField answerTextBox;

    @FXML
    private ProgressBar timeBar;

    @FXML
    private Label scoreLabel;

    @FXML
    private Group emoteGroup;

    @FXML
    private Button submit;

    @FXML
    private GridPane answerGridpane;

    private Boolean[] pressedEmote = {false, false, false, false, false};

    private GameState gameState;

    private String selectedAnswer;

    private List<LeaderboardEntry> leaderboardEntries;

    private final MainCtrl mainCtrl;

    private Timeline timeline;

    private boolean isSubmitted = false;

    boolean doubleUsed = false;

    boolean halfUsed = false;

    boolean eliminateUsed = false;

    boolean enableEliminateLater = false;

    boolean enableHalfLater = false;


    @Inject
    public QuestionCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    /**
     * Updates the gameState for this particular client
     *
     * @param gameState the new gameState
     */
    void updateGameState(GameState gameState) {
        this.gameState = gameState;
        switch (gameState.instruction) {
            case "questionPhase":
                isSubmitted = false;
                if(enableEliminateLater){//connected with disabling eliminateWrongAnswer in an open ended question
                    enableEliminateLater = false;
                    eliminateWrongAnswer.setDisable(false);
                }
                if(enableHalfLater&& !mainCtrl.singleplayerGame){//connected with disabling halfTime power up in a multiplayer game once its already been used
                    enableHalfLater = false;
                    halfTime.setDisable(false);
                }
                selectedAnswer = null;
                updateMultilayerLeaderboards();
                timeBar.setVisible(true);
                questionTime.setVisible(true);
                scoreLabel.setVisible(false);

                selectedCSS[0] = false;
                selectedCSS[1] = false;
                selectedCSS[2] = false;

                if(doubleUsed){
                    doublePoints.setDisable(true);
                }
                if(eliminateUsed){
                    eliminateWrongAnswer.setDisable(true);
                }
                if(halfUsed){
                    halfTime.setDisable(true);
                }
                this.questionTitle.setText("Question " + (gameState.currentQuestion + 1));
                if(gameState.currentQuestion + 1==1){//first question
                    if(mainCtrl.singleplayerGame){
                        halfTime.setDisable(true);
                    }else{
                        halfTime.setDisable(false);
                    }
                    eliminateWrongAnswer.setDisable(false);
                    doublePoints.setDisable(false);
                }
                this.questionText.setText(gameState.question.question);


                if (!gameState.question.type.equals("3")) {
                    this.answer1.setText(gameState.question.answer);
                    this.answer2.setText(gameState.question.wrongAnswer1);
                    this.answer3.setText(gameState.question.wrongAnswer2);
                    answer1.setVisible(true);
                    answer2.setVisible(true);
                    answer3.setVisible(true);
                } else {
                    //disable disable EliminateWrong power up
                    if(!eliminateWrongAnswer.isDisable()){
                        eliminateWrongAnswer.setDisable(true);
                        enableEliminateLater = true;
                    }

                    answerTextBox.clear();
                    answerTextBox.setVisible(true);
                    answerTextBox.setDisable(false);
                }

                timeline = new Timeline(new KeyFrame(Duration.millis(1), e -> {
                    double timeToDisplay = 10000 - (new Date().getTime() - gameState.timeOfReceival);
                    if (timeToDisplay < 0) {
                        questionTime.setText("Time left: 0.000 seconds");
                        timeBar.setProgress(0);
                        submit.setDisable(true);
                        answer1.setDisable(true);
                        answer2.setDisable(true);
                        answer3.setDisable(true);
                    } else {
                        questionTime.setText("Time left: " + String.format("%.3f", timeToDisplay / 1000.0) + " seconds");
                        timeBar.setProgress(timeToDisplay / 10000.0);
                    }
                }));
                timeline.setCycleCount(10000);
                timeline.play();

                answer1.getStyleClass().remove("selected");
                answer2.getStyleClass().remove("selected");
                answer3.getStyleClass().remove("selected");


                emotes.setVisible(true);
                emoteGroup.setVisible(true);

                if (mainCtrl.singleplayerGame) {
                    emotes.setVisible(false);
                    emoteGroup.setVisible(false);
                    allLeaderboard.setMaxHeight(165);
                }
                else {
                    allLeaderboard.setMaxHeight(380);
                }

                answer1.setDisable(false);
                answer2.setDisable(false);
                answer3.setDisable(false);
                submit.setDisable(false);

                break;
            case "halfTimePowerUp":
                if(!halfTime.isDisable()){
                    halfTime.setDisable(true);
                    enableHalfLater = true;
                }

                timeline = new Timeline(new KeyFrame(Duration.millis(1), e -> {
                    double timeToDisplay = 10000 - (new Date().getTime() - gameState.timeOfReceival);
                    if (timeToDisplay < 0) {
                        questionTime.setText("Time left: 0.000 seconds");
                        timeBar.setProgress(0);
                        submit.setDisable(true);
                        answer3.setDisable(true);
                        answer2.setDisable(true);
                        answer1.setDisable(true);
                    } else {
                        questionTime.setText("Time left: " + String.format("%.3f", timeToDisplay / 1000.0) + " seconds");
                        timeBar.setProgress(timeToDisplay / 10000.0);
                    }

                }));
                timeline.setCycleCount(10000);
                timeline.play();
                //TODO time is already being halved, but make it explicit to the client, so that it is easily noticeable
                break;
            case "score":
                timeBar.setVisible(false);
                questionTime.setVisible(false);
                scoreLabel.setText("You received: " + String.format("%.2f", gameState.thisScored * 10) + " points!");
                scoreLabel.setVisible(true);
                updateMultilayerLeaderboards();
        }


    }


    @FXML
    void Answer1Pressed(ActionEvent event) throws IOException, InterruptedException {
        if (!selectedCSS[0]) {
            answer1.getStyleClass().add("selected");
            answer2.getStyleClass().removeAll("selected");
            answer3.getStyleClass().removeAll("selected");
            selectedCSS = new boolean[]{true, false, false};
        }
        selectedAnswer = answer1.getText();
    }


    @FXML
    void Answer2Pressed(ActionEvent event) throws IOException, InterruptedException {
        if (!selectedCSS[1]) {
            answer2.getStyleClass().add("selected");
            answer1.getStyleClass().removeAll("selected");
            answer3.getStyleClass().removeAll("selected");
            selectedCSS = new boolean[]{false, true, false};
        }
        selectedAnswer = answer2.getText();
    }


    @FXML
    void Answer3Pressed(ActionEvent event) throws IOException, InterruptedException {
        if (!selectedCSS[2]) {
            answer3.getStyleClass().add("selected");
            answer2.getStyleClass().removeAll("selected");
            answer1.getStyleClass().removeAll("selected");
            selectedCSS = new boolean[]{false, false, true};
        }

        selectedAnswer = answer3.getText();
    }

    @FXML
    /**
     * Gets called when the submit answer button is pressed
     * Sends the answer to the server, together with the gameState
     */
    public void SubmitPressed(ActionEvent actionEvent) throws IOException, InterruptedException{
        if(isSubmitted) return;
        if(answerTextBox.isVisible()&&(answerTextBox.getText()==null||answerTextBox.getText().equals(""))) return;//if open ended and no answer, do not submit
        if((answer1.isVisible()||answer2.isVisible()||answer3.isVisible())&&selectedAnswer==null) return;

        isSubmitted = false;
        answer1.setDisable(true);
        answer2.setDisable(true);
        answer3.setDisable(true);
        submit.setDisable(true);

        answerTextBox.setDisable(true);


        if (answer1.isVisible()||answer2.isVisible()) {//if we have an MC question
            AnswerCommunication.sendAnswer(selectedAnswer, gameState, mainCtrl.playerCtrl.serverString);
        } else if (answerTextBox.isVisible()) {//if we have an open question
            selectedAnswer = answerTextBox.getText();
            AnswerCommunication.sendAnswer(selectedAnswer, gameState, mainCtrl.playerCtrl.serverString);
        } else {
            throw new IllegalArgumentException("submitPressed logic does not work properly");
        }

    }

    /**
     * Gets called when the Angry emote is pressed.
     * Sends the emote to the server and stops further identical emotes until the next question.
     */
    @FXML
    void AngryEmotePressed(ActionEvent event) throws IOException, InterruptedException {
        if (!pressedEmote[0]) {
            Emote emote = new Emote(Emote.Type.Angry, gameState.username, gameState.gameId);
            AnswerCommunication.sendEmote(emote, mainCtrl.playerCtrl.serverString);
            pressedEmote[0] = true;
        }
    }

    /**
     * Gets called when the LOL emote is pressed.
     * Sends the emote to the server and stops further identical emotes until the next question.
     */
    @FXML
    void LOLEmotePressed(ActionEvent event) throws IOException, InterruptedException {
        if (!pressedEmote[1]) {
            Emote emote = new Emote(Emote.Type.LOL, gameState.username, gameState.gameId);
            AnswerCommunication.sendEmote(emote, mainCtrl.playerCtrl.serverString);
            pressedEmote[1] = true;
        }
    }

    /**
     * Gets called when the Sweat emote is pressed.
     * Sends the emote to the server and stops further identical emotes until the next question.
     */
    @FXML
    void SweatEmotePressed(ActionEvent event) throws IOException, InterruptedException {
        System.out.println("Sweat emote pressed");
        if (!pressedEmote[2]) {
            Emote emote = new Emote(Emote.Type.Sweat, gameState.username, gameState.gameId);
            AnswerCommunication.sendEmote(emote, mainCtrl.playerCtrl.serverString);
            pressedEmote[2] = true;
        }
    }

    /**
     * Gets called when the Clap emote is pressed.
     * Sends the emote to the server and stops further identical emotes until the next question.
     */
    @FXML
    void ClapEmotePressed(ActionEvent event) throws IOException, InterruptedException {
        if (!pressedEmote[3]) {
            Emote emote = new Emote(Emote.Type.Clap, gameState.username, gameState.gameId);
            AnswerCommunication.sendEmote(emote, mainCtrl.playerCtrl.serverString);
            pressedEmote[3] = true;
        }
    }

    /**
     * Gets called when the Win emote is pressed.
     * Sends the emote to the server and stops further identical emotes until the next question.
     */
    @FXML
    void WinEmotePressed(ActionEvent event) throws IOException, InterruptedException {
        if (!pressedEmote[4]) {
            Emote emote = new Emote(Emote.Type.Win, gameState.username, gameState.gameId);
            AnswerCommunication.sendEmote(emote, mainCtrl.playerCtrl.serverString);
            pressedEmote[4] = true;
        }
    }


    @FXML
    /**
     * Gets called when the double points power up gets activated. Sends a request to the server,
     * where it is checked if the power up has already been used by this client. If not, the power
     * up is used and all players are alerted.
     */
    public void DoublePointsButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        if(gameState.stage== GameState.Stage.INTERVAL) return;
        doubleUsed = true;
        doublePoints.setDisable(true);

        String result = PowerUpsCommunication.sendPowerUps("doublePointsPowerUp", gameState, mainCtrl.playerCtrl.serverString);

        try {
            if (result.split("___")[1].equals("success")) {
                //TODO tell user that the powerup has been used properly
            } else {
                System.out.println("doublePointsPowerUp has already been used before");
                //TODO tell user power up is already used
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

    @FXML
    /**
     * Gets called when the eliminate wrong answer power up is used. Sends a request to the server,
     * where it is checked if the player has already used that power up. If not, the power up is used,
     * and the wrong answer button is made invisible and inaccessible. Also other players are alerted.
     */
    void EliminateWrongAnswerButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        if(gameState.stage== GameState.Stage.INTERVAL) return;
        eliminateUsed = true;
        eliminateWrongAnswer.setDisable(true);
        String result = PowerUpsCommunication.sendPowerUps("eliminateWrongAnswerPowerUp", gameState, mainCtrl.playerCtrl.serverString);

        try {
            if (result.split("___")[1].equals("success")) {
                String answer = result.split("___")[2];
                if (answer1.getText().equals(answer)) {
                    setVisible("answer1", false);
                }
                if (answer2.getText().equals(answer)) {
                    setVisible("answer2", false);
                }
                if (answer3.getText().equals(answer)) {
                    setVisible("answer3", false);
                }

            }
            if (result.split("___")[1].equals("fail")) {
                System.out.println("eliminateWrongAnswerPowerUp has already been used before");
                //TODO tell user that power up has already been used before

            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

    @FXML
    /**
     * Gets called when the half time power up is used. Sends a request to the server,
     * where it is checked if the player has already used that power up. If not, the power up is used,
     * and the time of all other players is halved.
     */
    void HalfTimeButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        if(gameState.stage== GameState.Stage.INTERVAL) return;
        halfUsed = true;
        halfTime.setDisable(true);
        String result = PowerUpsCommunication.sendPowerUps("halfTimePowerUp", gameState, mainCtrl.playerCtrl.serverString);
        try {
            if (result.split("___")[1].equals("success")) {
                System.out.println("halftime has been used but maybe not implemented yet");
                //TODO show to user that power up is used
            } else {
                System.out.println("halfTimePowerUp has already been used before");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }


    }

    @FXML
    void KeyPressed(KeyEvent event) throws IOException, InterruptedException {
        System.out.println(event.getCode() + " was pressed.");
        switch (event.getCode()) {
            case TAB:
                showLeaderboard();
                break;
            case ENTER:
                if(isSubmitted) return;
                SubmitPressed(new ActionEvent());
                break;
        }
    }

    @FXML
    void KeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case TAB:
                hideLeaderboard();
        }
    }

    @FXML
    void initialize() {
        assert questionImage != null : "fx:id=\"questionImage\" was not injected: check your FXML file 'Question.fxml'.";
        assert questionText != null : "fx:id=\"questionText\" was not injected: check your FXML file 'Question.fxml'.";
        assert questionTitle != null : "fx:id=\"questionTitle\" was not injected: check your FXML file 'Question.fxml'.";

        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            try {
                KeyPressed(event);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        root.addEventFilter(KeyEvent.KEY_RELEASED, this::KeyReleased);

        leaderboardRanks.setCellFactory(e -> {
            TableCell<LeaderboardEntry, String> indexCell = new TableCell<>();
            var rowProperty = indexCell.tableRowProperty();
            var rowBinding = createObjectBinding(() -> {
                TableRow<LeaderboardEntry> row = rowProperty.get();
                if (row != null) {
                    int rowIndex = row.getIndex();
                    if (rowIndex < row.getTableView().getItems().size()) {
                        return "#" + Integer.toString(rowIndex + 1);
                    }
                }
                return null;
            }, rowProperty);
            indexCell.textProperty().bind(rowBinding);
            return indexCell;
        });
        leaderboardUsernames.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().username));
        leaderboardScores.setCellValueFactory(e -> new SimpleStringProperty(String.format("%.2f", e.getValue().score)));
        emotesUsernameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().username));
        emotesEmoteColumn.setCellValueFactory(e -> {
            ImageView image = e.getValue().image;
            return new SimpleObjectProperty<>(image);
        });
        leaderboard.addEventFilter(ScrollEvent.ANY, Event::consume);
        hideLeaderboard();


        Map<String, Button> emoteTypes = new HashMap<>();
        emoteTypes.put("angry", emoteAngry);
        emoteTypes.put("lol", emoteLOL);
        emoteTypes.put("sweat", emoteSweat);
        emoteTypes.put("clap", emoteClap);
        emoteTypes.put("win", emoteWin);

        for (Map.Entry<String, Button> entry : emoteTypes.entrySet()) {
            String type = entry.getKey();
            Button button = entry.getValue();

            Image img = new Image("images/emojis/" + type + ".png");
            ImageView view = new ImageView(img);
            view.setFitHeight(30);
            view.setPreserveRatio(true);
            button.setText("");
            button.setPrefSize(50, 50);
            button.setGraphic(view);
        }
        emotes.setPlaceholder(new Label(""));

    }

    /**
     * Makes an FXML element visible/invisible
     *
     * @param variableName the fxml ID of the element
     * @param should       whether the element should become visible, or invisible
     */
    public void setVisible(String variableName, boolean should) {
        switch (variableName) {
            case "answer1":
                answer1.setVisible(should);
                break;
            case "answer2":
                answer2.setVisible(should);
                break;
            case "answer3":
                answer3.setVisible(should);
                break;
        }
    }



    /**
     * Sets the new Question for the Question phase.
     *
     * @param q the new Question
     */
    public void setQuestion(Question q, String serverString) {
        questionText.setText(q.question);//sets the question Text


        if (q.questionImage != null) {//sets the Image
            try {
                System.out.println("serverString is: " + serverString);
                System.out.println("serverString is: " + serverString);
                System.out.println("serverString is: " + serverString);
                questionImage.setImage(ImageCommunication.getImage(serverString + "/" + q.questionImage));
            } catch (IOException e) {
                System.out.println("Failed to set the question image.");
            }
        }

        try {
            if (!q.type.equals("3")) {//if this is a MC question
                answerGridpane.setVisible(true);
                List<String> answerList = new LinkedList<>(List.of(q.answer, q.wrongAnswer1, q.wrongAnswer2));
                Collections.shuffle(answerList);
                clearAnswer();
                answer1.setText(answerList.get(0));
                answer2.setText(answerList.get(1));
                answer3.setText(answerList.get(2));
                answerTextBox.setVisible(false);
            } else {//if this is an open question
                answerGridpane.setVisible(false);
                answer1.setVisible(false);
                answer2.setVisible(false);
                answer3.setVisible(false);
                clearAnswer();
                answerTextBox.setVisible(true);
                answerTextBox.setDisable(false);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("null pointer again????");
        }

        for (int i = 0; i < 5; i++) {//Reset the emotes so they can be pressed again for the new question.
            pressedEmote[i] = false;
        }


    }

    public void markAnswer(String correct, String ofplayer, String type) {
        if (type.equals("3")) {
            answerTextBox.setText(correct);
            answerTextBox.setDisable(true);
        } else {
            for (Button answer : List.of(answer1, answer2, answer3)) {
                answer.getStyleClass().removeAll("wrong", "right", "default");
                if (answer.getText().equals(correct)) {
                    answer.getStyleClass().add("right");
                } else if (answer.getText().equals(ofplayer)) {
                    answer.getStyleClass().add("wrong");
                } else {
                    answer.getStyleClass().add("default");
                }
            }
        }
    }

    public void clearAnswer() {
        for (Button answer : List.of(answer1, answer2, answer3)) {
            answer.getStyleClass().removeAll("wrong", "right", "default");
            answer.getStyleClass().add("default");
        }
    }

    public void showLeaderboard() {
        // if the current list of player in the lobby is one then the current game is in multiplayer mode
        if (mainCtrl.singleplayerGame == false) {

            System.out.println("SIZE FOR LEADERBOARD: " + leaderboardEntries.size());
            ObservableList<LeaderboardEntry> entries = FXCollections.observableList(leaderboardEntries);
            leaderboard.setItems(entries);
            allLeaderboard.setVisible(true);
        }
        // if the current list of player in the lobby is one then the current game is  in single player mode
        if (mainCtrl.singleplayerGame == true) {

            ObservableList<LeaderboardEntry> entries = FXCollections.observableList(leaderboardEntries);
            leaderboard.setItems(entries);
            //updateCurrentPlayer(gameState);
            allLeaderboard.setVisible(true);
        }
    }

    public void hideLeaderboard() {
        allLeaderboard.setVisible(false);
    }

    /**
     * Retrieves the list of Leaderboard Entries from the server and populates the table in the game
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void updateMultilayerLeaderboards() {
        System.out.println("UPDATE OF LEADERBOARD IS HAPPENING");

        try {
            leaderboardEntries = mainCtrl.getMultiplayerLeaderboards();
            Collections.sort(leaderboardEntries);
            List<LeaderboardEntry> auxiliarList = new ArrayList<>();
            for (int i = 0; i <= 4 && i < leaderboardEntries.size(); i++) {
                auxiliarList.add(leaderboardEntries.get(i));
            }
            int currentPlayerPosition = -1;
            for (int i = 0; i < leaderboardEntries.size(); i++) {
                if (gameState.username.equals(leaderboardEntries.get(i).username)) {
                    currentPlayerPosition = i;
                    break;
                }
            }
            System.out.println("PLAYER POSITION E: " + currentPlayerPosition);
            if (currentPlayerPosition != -1) {
                if (currentPlayerPosition <= 4) {
                    System.out.println("PLAYER POSITION E FALS");
                    playerPosition.setVisible(false);
                } else {
                    updateCurrentPlayer(gameState);
                }
            }
            leaderboardEntries = auxiliarList;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void updateSingleplayerLeaderboards() {

        try {
            leaderboardEntries = mainCtrl.getSingleplayerLeaderboards();
            Collections.sort(leaderboardEntries);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void updateEmotes(List<Emote> emoteEntries) {
        //TODO: Fix cell so that the image of the emote is displayed
        List<EmoteEntry> emoteEntriesWithImage = new ArrayList<>();
        for (Emote emote : emoteEntries) {

            Image img = new Image("images/emojis/" + emote.type + ".png");
            ImageView imageView = new ImageView(img);
            imageView.setFitHeight(30);
            imageView.setPreserveRatio(true);

            emoteEntriesWithImage.add(new EmoteEntry(emote.username, imageView));
        }
        System.out.println(emoteEntriesWithImage);
        ObservableList<EmoteEntry> emoteEntriesList = FXCollections.observableList(emoteEntriesWithImage);
        this.emotes.setItems(emoteEntriesList);
    }

    /**
     * Allows the current user to see their own scores and ranks at the bottom of the leaderboard
     *
     * @param state
     */
    public void updateCurrentPlayer(GameState state) {
        for (int i = 0; i < leaderboardEntries.size(); i++) {
            LeaderboardEntry e = leaderboardEntries.get(i);
            if (e.username.equals(state.username)) {
                playerUsername.setText(e.username);
                playerScore.setText(String.valueOf(e.score));
                playerRank.setText("#" + String.valueOf(i + 1));
                break;
            }
        }
    }
}

class EmoteEntry {

    public String username;
    public ImageView image;

    public EmoteEntry(String username, ImageView image) {
        this.username = username;
        this.image = image;
    }

    public void setImage(ImageView value) {
        image = value;
    }

    public ImageView getImage() {
        return image;
    }

}

