package org.getmonero.i2p.zero.gui;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.getmonero.i2p.zero.RouterWrapper;
import static org.getmonero.i2p.zero.TunnelControl.Tunnel;

import java.text.DecimalFormat;
import java.util.Properties;

public class Controller {

  private RouterWrapper routerWrapper;

  @FXML
  private BorderPane rootBorderPane;
  @FXML private Slider bandwidthSlider;
  @FXML private Label maxBandwidthLabel;
  @FXML private ImageView masterToggle;
  @FXML private AnchorPane bandwidthDisabledOverlay;
  @FXML private Tab bandwidthTab;
  @FXML private Tab tunnelsTab;
  @FXML private Tab helpTab;
  @FXML private Label statusLabel;
  @FXML private Button tunnelAddButton;
  @FXML private Button tunnelRemoveButton;
  @FXML private TableView<Tunnel> tunnelsTableView;
  @FXML private TableColumn typeCol;
  @FXML private TableColumn hostCol;
  @FXML private TableColumn portCol;
  @FXML private TableColumn i2PCol;
  @FXML private Label bandwidthIn1s;
  @FXML private Label bandwidthIn5m;
  @FXML private Label bandwidthInAll;
  @FXML private Label totalTransferredIn;
  @FXML private Label bandwidthOut1s;
  @FXML private Label bandwidthOut5m;
  @FXML private Label bandwidthOutAll;
  @FXML private Label totalTransferredOut;

  DecimalFormat format2dp = new DecimalFormat("0.00");
  private boolean masterState = true;
  public final ObservableList<Tunnel> tunnelTableList = FXCollections.observableArrayList();

  private Stage getStage() {
    return (Stage) rootBorderPane.getScene().getWindow();
  }

  @FXML private void initialize() {

    typeCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("type"));
    hostCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("host"));
    portCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("port"));
    i2PCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("I2P"));

    DoubleBinding usedWidth = typeCol.widthProperty().add(hostCol.widthProperty()).add(portCol.widthProperty());
    i2PCol.prefWidthProperty().bind(tunnelsTableView.widthProperty().subtract(usedWidth).subtract(2));

    tunnelsTableView.setItems(tunnelTableList);

    tunnelsTableView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldSelection, newSelection) -> {
      if (newSelection == null) {
        tunnelRemoveButton.setDisable(true);
      }
      else {
        tunnelRemoveButton.setDisable(false);
      }
    });

    tunnelRemoveButton.setOnAction(e->{
      Tunnel t = tunnelsTableView.getSelectionModel().getSelectedItem();
      getRouterWrapper().getTunnelControl().getTunnels().remove(t);
      tunnelTableList.remove(t);
      t.destroy();
      tunnelRemoveButton.setDisable(true);
    });

    tunnelAddButton.setOnAction(event->{
      try {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(getStage());
        dialogStage.setResizable(false);
        dialogStage.setTitle("New tunnel");
        Scene dialogScene = new Scene(FXMLLoader.load(getClass().getResource("addTunnel.fxml")));
        dialogScene.getStylesheets().add("org/getmonero/i2p/zero/gui/gui.css");
        dialogStage.setScene(dialogScene);
        dialogStage.show();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    EventHandler<Event> tabSelectionEventHandler = e->{
      Stage stage = getStage();
      if(bandwidthTab.isSelected()) {
        stage.setWidth(360);
        stage.setHeight(370);
      }
      else if(tunnelsTab.isSelected()) {
        stage.setWidth(700);
      }
    };
    bandwidthTab.setOnSelectionChanged(tabSelectionEventHandler);
    tunnelsTab.setOnSelectionChanged(tabSelectionEventHandler);
    helpTab.setOnSelectionChanged(tabSelectionEventHandler);

    bandwidthSlider.valueProperty().addListener((observableValue, oldValue, newValue)-> {
      maxBandwidthLabel.setText(String.format("%.1f", newValue.floatValue()) + " Mbps");
      routerWrapper.debouncedUpdateBandwidthLimitKBPerSec((int) Math.round(1024d*newValue.doubleValue()/8d));
    });

    masterToggle.setOnMouseClicked(e->{
      masterState = !masterState;
      bandwidthDisabledOverlay.setVisible(!masterState);

      if(masterState) {
        masterToggle.setImage(new Image("org/getmonero/i2p/zero/gui/toggle-on.png"));
        statusLabel.setVisible(true);
        routerWrapper.start();
        tunnelAddButton.setDisable(false);
      }
      else {
        masterToggle.setImage(new Image("org/getmonero/i2p/zero/gui/toggle-off.png"));
        statusLabel.setVisible(false);
        routerWrapper.stop();
        tunnelTableList.clear();
        tunnelAddButton.setDisable(true);
      }
    });

    startRouter();

    var bandwidthUpdateThread = new Thread(()->{
      while(!Gui.instance.isStopping()) {
        if (routerWrapper.isStarted()) {
          Platform.runLater(() -> {
            bandwidthIn1s.setText(format2dp.format(routerWrapper.get1sRateInKBps()) + " KBps");
            bandwidthIn5m.setText(format2dp.format(routerWrapper.get5mRateInKBps()) + " KBps");
            bandwidthInAll.setText(format2dp.format(routerWrapper.getAvgRateInKBps()) + " KBps");
            totalTransferredIn.setText(format2dp.format(routerWrapper.getTotalInMB()) + " MB   ");
            bandwidthOut1s.setText(format2dp.format(routerWrapper.get1sRateOutKBps()) + " KBps");
            bandwidthOut5m.setText(format2dp.format(routerWrapper.get5mRateOutKBps()) + " KBps");
            bandwidthOutAll.setText(format2dp.format(routerWrapper.getAvgRateOutKBps()) + " KBps");
            totalTransferredOut.setText(format2dp.format(routerWrapper.getTotalOutMB()) + " MB   ");

            statusLabel.setText("Status: " + routerWrapper.getReachability().getMessage());

          });
        }
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
      }
    });
    bandwidthUpdateThread.start();

    Runtime.getRuntime().addShutdownHook(new Thread(()->bandwidthUpdateThread.interrupt()));

  }

  public RouterWrapper getRouterWrapper() {
    return routerWrapper;
  }

  private int getBandwidthLimitKBPerSec() {
    return (int)Math.round(bandwidthSlider.getValue()*1024d/8d);
  }

  private void startRouter() {

    // need to launch Gui with parameters: --i2p.dir.base= and --i2p.dir.config=

    var params = Gui.instance.getParameters().getNamed();

    Properties routerProperties = new Properties();
    routerProperties.put("i2p.dir.base", params.get("i2p.dir.base"));
    routerProperties.put("i2p.dir.config", params.get("i2p.dir.config"));
    routerProperties.put("i2np.inboundKBytesPerSecond", getBandwidthLimitKBPerSec());
    routerProperties.put("i2np.outboundKBytesPerSecond", getBandwidthLimitKBPerSec());
    routerProperties.put("router.sharePercentage", 80);
    routerWrapper = new RouterWrapper(routerProperties);
    routerWrapper.start();

  }


}
