package main.java.controller;

import com.jfoenix.controls.JFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import main.java.misc.BackgroundProcesses;
import main.java.misc.InputRestrictor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

public class POSRestock extends POSInventory{

    @FXML
    private StackPane rootPane;

    @FXML
    private TextField tfItemCode;

    @FXML
    private TextField tfItemName;

    @FXML
    private TextField tfCurrentStock;

    @FXML
    private TextField tfAddStock;

    @FXML
    private Label lblEstimatedValue;

    @FXML
    private JFXButton btnCancel;

    @FXML
    private JFXButton btnSave;

    @FXML
    private JFXButton btnAdd,btnSubtract;

    protected double price = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Scanner scan = new Scanner(new FileInputStream(BackgroundProcesses.getFile("etc\\cache-selected-item.file")));
            scan.nextLine();
            tfItemCode.setText(scan.nextLine());
            tfItemName.setText(scan.nextLine());
            price = Double.parseDouble(scan.nextLine());
            tfCurrentStock.setText(scan.nextLine());
            lblEstimatedValue.setText(scan.nextLine());

            tfAddStock.setText(tfCurrentStock.getText());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        InputRestrictor.numbersInput(tfAddStock);
        InputRestrictor.limitInput(tfAddStock,3);
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            int newVal = tfAddStock.getText().equals("") ? 0 : Integer.parseInt(tfAddStock.getText());
            lblEstimatedValue.setText((newVal*price)+"");
        }),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
    //TODO recalculating the Text everytime the user are entering new stock value
    //TODO update database when adding stocks

    @FXML
    void btnCancelOnAction(ActionEvent event) {
        sceneManipulator.closeDialog();
    }

    @FXML
    void btnSaveOnAction(ActionEvent event) {

    }

    @FXML
    private void changeStockButton(ActionEvent event) {
        if (tfAddStock.getText().isEmpty())
            return;
        else if (tfAddStock.getText().equals("1") && event.getSource().equals(btnSubtract))
            return;

        var x = Integer.parseInt(tfAddStock.getText());
        if (event.getSource().equals(btnAdd))
            x=x+1;
        else if (event.getSource().equals(btnSubtract))
            x=x-1;
        tfAddStock.setText(String.valueOf(x));

    }
}
