package client.scenes;

import client.Communication.AnswerCommunication;
import client.Communication.ImageCommunication;
import client.Communication.PowerUpsCommunication;
import com.google.inject.Inject;
import commons.GameState;
import commons.LeaderboardEntry;
import commons.Question;
import commons.Timer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

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

    @FXML
    private Button doublePoints;

    @FXML
    private Button eliminateWrongAnswer;

    @FXML
    private Button halfTime;


    @FXML
    private ImageView questionImage;

    @FXML
    private Label questionText;

    @FXML
    private Label questionTitle;

    @FXML
    private Label questionTime;

    @FXML
    private TableView<LeaderboardEntry> leaderboard;

    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardUsernames;
    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardRanks;
    @FXML
    private TableColumn<LeaderboardEntry, String> leaderboardScores;

    @FXML
    private AnchorPane root;

    private Timer timer;

    private GameState gameState;

    private String selectedAnswer;

    private List<LeaderboardEntry> leaderboardEntries;


    private final MainCtrl mainCtrl;

    @Inject
    public QuestionCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    void updateGameState(GameState gameState) {
        this.gameState = gameState;

        //TODO: Update question number based on current question
        this.questionTitle.setText("Question 10");
        this.questionText.setText(gameState.question.question);

        this.answer1.setText(gameState.question.answer);
        this.answer2.setText(gameState.question.wrongAnswer1);
        this.answer3.setText(gameState.question.wrongAnswer2);


    }

    //TODO: Send correct Game ID
    @FXML
    void Answer1Pressed(ActionEvent event) throws IOException, InterruptedException {
        selectedAnswer = answer1.getText();
    }
    //TODO: Send correct Game ID
    @FXML
    void Answer2Pressed(ActionEvent event) throws IOException, InterruptedException {
        selectedAnswer = answer2.getText();
    }

    //TODO: Send correct Game ID
    @FXML
    void Answer3Pressed(ActionEvent event) throws IOException, InterruptedException {
        selectedAnswer = answer3.getText();
    }

    @FXML
    public void SubmitPressed(ActionEvent actionEvent) throws IOException, InterruptedException {
        AnswerCommunication.sendAnswer(selectedAnswer, gameState);
    }


    @FXML
    void DoublePointsButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        PowerUpsCommunication.sendPowerUps(doublePoints.getText() + " WAS USED!");
    }

    @FXML
    void EliminateWrongAnswerButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        PowerUpsCommunication.sendPowerUps(eliminateWrongAnswer.getText()+ " WAS USED!");
    }

    @FXML
    void HalfTimeButtonPressed(ActionEvent event) throws IOException, InterruptedException {
        PowerUpsCommunication.sendPowerUps(halfTime.getText() +" WAS USED!");
    }

    @FXML
    void KeyPressed(KeyEvent event){
        System.out.println(event.getCode() + " was pressed.");
        switch (event.getCode()) {
            case TAB:
                showLeaderboard();
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

        root.addEventFilter(KeyEvent.KEY_PRESSED, this::KeyPressed);
        root.addEventFilter(KeyEvent.KEY_RELEASED, this::KeyReleased);

        timer = new Timer(0,5);
        Timeline timeline= new Timeline( new KeyFrame(Duration.millis(1), e ->{
            questionTime.setText(timer.toTimerDisplayString());
        }));
        timeline.setCycleCount((int)timer.getDurationLong()/1000);
        timeline.play();

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
        leaderboardScores.setCellValueFactory(e -> new SimpleStringProperty(Integer.toString(e.getValue().score)));

        hideLeaderboard();
        }

    public void syncTimer(long syncLong, long duration) {
        timer.setDuration(duration);
        timer.synchronize(syncLong);
    }

    public void setQuestion(Question q) {
        questionText.setText(q.question);
        if (q.questionImage != null) {
            try {
                questionImage.setImage(ImageCommunication.getImage("https://localhost:8080/" + q.questionImage));
            }
            catch (IOException e) {
                System.out.println("Failed to set the question image.");
            }
        }
        List<String> answerList = new LinkedList<>(List.of(q.answer, q.wrongAnswer1, q.wrongAnswer2));

        Collections.shuffle(answerList);

        clearAnswer();
        answer1.setText(answerList.get(0));
        answer2.setText(answerList.get(1));
        answer3.setText(answerList.get(2));
    }

    public void markAnswer(String correct, String ofplayer) {
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

    public void clearAnswer() {
        for (Button answer : List.of(answer1, answer2, answer3)) {
            answer.getStyleClass().removeAll("wrong", "right", "default");
            answer.getStyleClass().add("default");
        }
    }

    public void showLeaderboard() {
        try{
            // if the current list of player in the lobby is one then the current game is in multiplayer mode
        if (mainCtrl.getPlayers().size() > 1) {
            updateMultilayerLeaderboards();

            ObservableList<LeaderboardEntry> entries = FXCollections.observableList(leaderboardEntries);
            leaderboard.setItems(entries);
            leaderboard.setVisible(true);
        }
        // if the current list of player in the lobby is one then the current game is  in single player mode
        if (mainCtrl.getPlayers().size() ==1){
            updateSingleplayerLeaderboards();

            ObservableList<LeaderboardEntry> entries = FXCollections.observableList(leaderboardEntries);
            leaderboard.setItems(entries);
            leaderboard.setVisible(true);

        }
    }
        catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void hideLeaderboard() {
        leaderboard.setVisible(false);
    }

    /**
     * Retrieves the list of Leaderboard Entries from the server and populates the table in the game
     * @throws IOException
     * @throws InterruptedException
     */
    public void updateMultilayerLeaderboards(){

        try {
            leaderboardEntries = mainCtrl.getMultiplayerLeaderboards();
            Collections.sort(leaderboardEntries);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    public void updateSingleplayerLeaderboards(){

        try {
            leaderboardEntries = mainCtrl.getSingleplayerLeaderboards();
            Collections.sort(leaderboardEntries);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}