package main.java.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import main.java.MiscInstances;
import main.java.data.entity.Item;
import main.java.data.entity.ProductOrder;
import main.java.misc.BackgroundProcesses;
import main.java.misc.DirectoryHandler;
import main.java.misc.InputRestrictor;
import main.java.misc.SceneManipulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Scanner;

public class POSCashier implements Initializable {

    /*************************************************/
    /*********** UI COMPONENT VARIABLES **************/
    /*************************************************/
    @FXML
    private StackPane rootPane;

    @FXML
    private JFXTreeTableView<ProductOrder> ttvOrderList;

    @FXML
    private TreeTableColumn<ProductOrder, String> chProduct;

    @FXML
    private TreeTableColumn<ProductOrder, String> chProductID;


    @FXML
    private TreeTableColumn<ProductOrder, Double> chUnitPrice;

    @FXML
    private TreeTableColumn<ProductOrder, Integer> chQuantity;

    @FXML
    private TreeTableColumn<ProductOrder, Double> chTotal;

    @FXML
    private JFXButton btnHome;

    @FXML
    private Label lblDate;

    @FXML
    private ImageView ivAdmin;

    @FXML
    private ImageView ivGsmSignal;

    @FXML
    private ImageView ivRfidSignal;

    @FXML
    private Label lblProductName;

    @FXML
    private Label lblBarcodeNumber;

    @FXML
    private Label lblUnitPrice;

    @FXML
    private JFXButton btnSubtract;

    @FXML
    private TextField tfQuantity;

    @FXML
    private JFXButton btnAdd;

    @FXML
    private JFXButton btnRemove;

    @FXML
    private JFXButton btnReturn;

    @FXML
    private JFXButton btnDiscount;

    @FXML
    private JFXButton btnAddCredits;

    @FXML
    private JFXButton btnRemoveAll;

    @FXML
    private JFXButton btnPriceInquiry;

    @FXML
    private JFXButton btnScanItem;

    @FXML
    private Label lblNumberItem;

    @FXML
    private Label lblSubtotal;

    @FXML
    private Label lblDiscount;

    @FXML
    private Label lblTax;

    @FXML
    private Label lblTotal;

    @FXML
    private JFXButton btnCheckout;





    /*************************************************/
    /****************** VARIABLES ********************/
    /*************************************************/

    protected static ObservableList<ProductOrder> productList = FXCollections.observableArrayList();
    protected static ArrayList <Item>allItem = new ArrayList<Item>();

    private double total = 0;
    private int items = 0;
    private ProductOrder selectedProduct = null;
    protected MiscInstances misc;

    protected static POSDialog dialog;// static dialog to make it accessible
                            // to the Dialog that is currently open
                            // and easy to access the close method of the Dialog

    protected static final SceneManipulator sceneManipulator = new SceneManipulator();


    /*************************************************/
    /*************** EVENT HANDLERS ******************/
    /*************************************************/
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        misc = new MiscInstances();
        queryAllItem();
        InputRestrictor.numbersInput(this.tfQuantity);
        InputRestrictor.limitInput(this.tfQuantity,3);
        BackgroundProcesses.realTimeClock(lblDate);
        loadTable();
        try {
            Scanner scan = new Scanner(new FileInputStream("etc\\cache-user.file"));
            scan.nextLine();
            scan.nextLine();
            scan.nextLine();
            scan.nextLine();
            ivAdmin.setImage(scan.nextLine().equals("1")
                    ? new Image(DirectoryHandler.IMG+"pos-admin.png")
                    : new Image(DirectoryHandler.IMG+"pos-admin-disable.png") );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        totalCountRefresher();
        itemCountRefresher();
    }


    @FXML
    protected void btnHomeOnAction(ActionEvent event) {
        sceneManipulator.changeScene(rootPane,"POSDashboard", "Dashboard");
    }

    @FXML
    protected void btnCheckoutOnAction(ActionEvent event) {
        if (event.getSource().equals(this.btnCheckout))
            sceneManipulator.openDialog(rootPane,"POSCheckout");
    }

    @FXML
    protected void btnFunctionalitiesOnAction(ActionEvent event) {
        JFXButton selectedButton = (JFXButton) event.getSource();
        if (selectedButton.equals(this.btnScanItem)){
            sceneManipulator.openDialog(rootPane,"POSScanItem");
        }else if (selectedButton.equals(this.btnDiscount)){
            sceneManipulator.openDialog(rootPane,"POSDiscount");
        }else if (selectedButton.equals(this.btnRemoveAll)){
            productList.clear();
        }else if (selectedButton.equals(this.btnPriceInquiry)){
            sceneManipulator.openDialog(rootPane,"POSPriceInquiry");
        }

    }

    @FXML
    protected void btnQuantityChangerOnAction(ActionEvent event) {
        if (tfQuantity.getText().isEmpty())
            return;
        else if (tfQuantity.getText().equals("1") && event.getSource().equals(btnSubtract))
            return;

        var x = Integer.parseInt(tfQuantity.getText());
        if (event.getSource().equals(btnAdd))
            x=x+1;
        else if (event.getSource().equals(btnSubtract))
            x=x-1;
        tfQuantity.setText(String.valueOf(x));

        int newQuantity = Integer.parseInt(tfQuantity.getText());
        changeQuantityOnTable(newQuantity);

    }

    @FXML
    protected void btnRemove(ActionEvent event) {
        String id[] =lblBarcodeNumber.getText().split(": ");
        if (id.length>1){
            productList.forEach(e->{
                if (e.getProductID().equals(id[1]))
                    selectedProduct = e;
            });
            productList.remove(selectedProduct);
            lblProductName.setText("Product: ");
            lblBarcodeNumber.setText("Product ID: ");
            lblUnitPrice.setText("Unit Price: ");
            tfQuantity.setText("");
            totalCountRefresher();
            itemCountRefresher();
        }
    }

    @FXML
    protected void ttvOrderOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)
            populateProductInformation();
    }

    @FXML
    protected void ttvOrderOnMouseClicked(MouseEvent event) {
        populateProductInformation();
    }




    /*************************************************/
    /*********** FUNCTIONS AND PROCEDURES ************/
    /*************************************************/


    private void populateProductInformation(){
        var treeItem = ttvOrderList.getSelectionModel().getSelectedItem();
        if (treeItem!=null){
            var prod = treeItem.getValue();
            lblProductName.setText("Product: "+prod.getProduct());
            lblBarcodeNumber.setText("Product ID: "+prod.getProductID());
            lblUnitPrice.setText("Unit Price: "+prod.getUnitPrice());
            tfQuantity.setText(prod.getQuantity()+"");
        }
    }

    private void totalCountRefresher(){//for refreshing the total counter
        Timeline totalCountRefresher = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            totalCount();
        }),new KeyFrame(Duration.millis(100)));
        totalCountRefresher.setCycleCount(Animation.INDEFINITE);
        totalCountRefresher.play();
    }

    private void itemCountRefresher(){//for refreshing the item counter
        Timeline itemCountRefresher = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            itemCount();
        }),new KeyFrame(Duration.millis(100)));
        itemCountRefresher.setCycleCount(Animation.INDEFINITE);
        itemCountRefresher.play();
    }

    private void totalCount(){//for calculating the total of amount of the products
        total = 0;
        productList.forEach((e)->{
            total+=e.getTotal();
        });

        lblTotal.setText(total+"");
    }

    private void itemCount(){ //for counting the overall number of items
        items = 0;
        productList.forEach((e)->{
            items+=e.getQuantity();
        });
        lblNumberItem.setText(items+"");
    }

    private void openDialog(String fxml){
        try {
            Parent parent = FXMLLoader.load(getClass().getResource("/"+ DirectoryHandler.FXML+fxml+".fxml"));
            dialog = new POSDialog(rootPane, (Pane) parent,false);
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeQuantityOnTable(int newQuantity){
        String id[] =lblBarcodeNumber.getText().split(": ");
        if (id.length<1)return;
        productList.forEach((e)->{
            if (e.getProductID().equals(id[1]))
                e.setQuantity(newQuantity);
        });
    }

    private void loadTable(){
        chProductID.setCellValueFactory(new TreeItemPropertyValueFactory<ProductOrder,String>("productID"));
        chProduct.setCellValueFactory(new TreeItemPropertyValueFactory<ProductOrder,String>("product"));
        chUnitPrice.setCellValueFactory(new TreeItemPropertyValueFactory<ProductOrder,Double>("unitPrice"));
        chQuantity.setCellValueFactory(new TreeItemPropertyValueFactory<ProductOrder,Integer>("quantity"));
        chTotal.setCellValueFactory(new TreeItemPropertyValueFactory<ProductOrder,Double>("total"));
        TreeItem <ProductOrder>dataItem = new RecursiveTreeItem<ProductOrder>(productList, RecursiveTreeObject::getChildren);
        ttvOrderList.setRoot(dataItem);
        ttvOrderList.setShowRoot(false);
    }

    private void queryAllItem(){
        String sql = "Select * from Item";
        misc.dbHandler.startConnection();
        ResultSet result = misc.dbHandler.execQuery(sql);
        try{
            while(result.next()){
                Item item = new Item(result.getInt("itemID")
                        ,result.getString("itemCode")
                        ,result.getString("itemName")
                        ,result.getDouble("itemPrice")
                        ,result.getInt("stock"),new JFXButton(),new HBox());
                allItem.add(item);
            }
            misc.dbHandler.closeConnection();
        }catch (Exception e){
            e.printStackTrace();
            misc.dbHandler.closeConnection();
        }

    }


    /*************************************************/
    /******** STATIC FUNCTIONS AND PROCEDURES ********/
    /*************************************************/
    protected static void addItemToList(ProductOrder productOrder){
        productList.add(productOrder);
    }
}
