package main.java.controller;

import com.jfoenix.controls.JFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import main.java.Main;
import main.java.controller.message.POSMessage;
import main.java.data.AES;
import main.java.misc.BackgroundProcesses;
import main.java.misc.DirectoryHandler;

import java.io.*;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Scanner;

public class POSCheckout extends POSCashier {

    @FXML
    private StackPane rootPane;

    @FXML
    private Label lblCardID;

    @FXML
    private Label lblOwner;

    @FXML
    private Label lblBalance;

    @FXML
    private Label lblCheckout;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblTotal;

    @FXML
    private ImageView ivPrompt;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BackgroundProcesses.changeSecondaryFormStageStatus((short) 1);
        scanCard();
    }

    @FXML
    void btnCancelOnAction(ActionEvent event) throws IOException {
        cacheClear();
        Main.rfid.cancelOperation();
        BackgroundProcesses.changeSecondaryFormStageStatus((short) 0);
        sceneManipulator.closeDialog();
    }

    @FXML
    void btnProceedOnAction(ActionEvent event) {

    }

    private Timeline cardIdScannerThread, checkPINThread;
    private String cardID=null,customerID=null;
    private void scanCard(){
        try {
            Main.rfid.scanBasic();
            cardIdScannerThread = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                try {
                    Scanner scan = new Scanner(new FileInputStream("etc\\rfid-cache.file"));
                    while (scan.hasNextLine()){
                        String scanned[] = scan.nextLine().split("=");
                        if (scanned[0].equals("scanBasic")){
                            cardID = scanned[1];
                            queryCard();
                            Main.rfid.clearCache();
                            cardIdScannerThread.stop();
                            break;
                        }

                    }
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }),
                    new KeyFrame(Duration.seconds(1))
            );
            cardIdScannerThread.setCycleCount(Animation.INDEFINITE);
            cardIdScannerThread.play();
        }catch (NullPointerException e){
            JFXButton button = new JFXButton("Ok");
            button.setOnAction(s->{
                POSMessage.closeMessage();
            });
            POSMessage.showConfirmationMessage(rootPane,"Please connect the RFID Scanner to complete Task",
                    "Cannot Detect Scanner",
                    POSMessage.MessageType.ERROR,button);
        }
    }
    private Scanner scan;
    private String forChallenge;
    private void checkPIN() throws FileNotFoundException {

        scan = new Scanner(new FileInputStream("etc\\cache-checkout-card.file"));
        for (int i  = 1; i<=6;i++) System.out.println(scan.nextLine());
        forChallenge= AES.decrypt(scan.nextLine(),POSCashier.S_KEY);//TODO Under observation
        Main.rfid.challenge(forChallenge);

        checkPINThread = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            try {
                scan = new Scanner(new FileInputStream("etc\\rfid-cache.file"));
                while (scan.hasNextLine()){
                    String scanned[] = scan.nextLine().split("=");
                    if (scanned[0].equals("challengeResult")){
                        if (scanned[1].equals("1")){
                            populateData();
                            Main.rfid.clearCache();
                            lblStatus.setText("Processing transaction...");
                            ivPrompt.setImage(new Image(DirectoryHandler.IMG+"pos-spinner.gif"));
                            checkPINThread.stop();
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                checkPINThread.stop();
            }

        }),
                new KeyFrame(Duration.seconds(1))
        );
        checkPINThread.setCycleCount(Animation.INDEFINITE);
        checkPINThread.play();

    }

    private BufferedWriter writer;
    private void queryCard(){
        String sql = "Select * from card where cardID='"+cardID+"' and isActive = 1";
        misc.dbHandler.startConnection();
        ResultSet result = misc.dbHandler.execQuery(sql);

        String data="";
        try {
            if (result.next()){
                data+=result.getString("cardId")+"\n"
                    + result.getDouble("credits")+"\n"
                    + result.getInt("isActive")+"\n"
                    + result.getString("activationDate")+"\n"
                    + result.getString("expiryDate")+"\n"
                    + result.getInt("customerID")+"\n"
                    + result.getString("PIN");

                customerID = result.getInt("customerID")+"";
                writer = new BufferedWriter(new FileWriter("etc\\cache-checkout-card.file"));
                writer.write(data);
                writer.close();
                misc.dbHandler.closeConnection();

                sql = "Select * from customer where customerID = "+customerID+"";
                misc.dbHandler.startConnection();
                result = misc.dbHandler.execQuery(sql);
                result.next();
                data="";
                data += result.getInt("customerID")+"\n"+
                        result.getString("firstName")+"\n"+
                        result.getString("middleInitial")+"\n"+
                        result.getString("lastName")+"\n"+
                        result.getString("Sex")+"\n"+
                        result.getString("address")+"\n"+
                        result.getString("phoneNumber")+"\n"+
                        result.getString("emailAddress");
                writer = new BufferedWriter(new FileWriter("etc\\cache-checkout-customer.file"));
                writer.write(data);
                writer.close();
                misc.dbHandler.closeConnection();

                checkPIN();
            }else{
                JFXButton button = new JFXButton("Ok");
                button.setOnAction(s->{
                    POSMessage.closeMessage();
                    scanCard();
                });
                POSMessage.showConfirmationMessage(rootPane,"Card doesn't exist or maybe\nit is deactivated",
                        "Invalid Card",
                        POSMessage.MessageType.ERROR,button);
                misc.dbHandler.closeConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void populateData() throws IOException {

        scan = new Scanner(new FileInputStream("etc\\cache-checkout-card.file"));
        lblCardID.setText(scan.nextLine());
        lblBalance.setText(scan.nextLine());
        scan = new Scanner(new FileInputStream("etc\\cache-checkout-customer.file"));
        scan.nextLine();
        lblOwner.setText(scan.nextLine()+" "+scan.nextLine()+". "+scan.nextLine());
        scan = new Scanner(new FileInputStream("etc\\cache-checkout-total.file"));
        if (scan.hasNextLine())
            lblCheckout.setText(scan.nextLine());

        lblTotal.setText(String.valueOf(Double.parseDouble(lblBalance.getText())
                - Double.parseDouble(lblCheckout.getText())));

        writer = new BufferedWriter(new FileWriter("etc\\cache-secondary-check-card.file"));
        writer.write("1");
        writer.close();
    }


    private void cacheClear() throws IOException {
        writer = new BufferedWriter(new FileWriter("etc\\cache-secondary-check-card.file"));
        writer.write("0");
        writer.close();
    }
}
