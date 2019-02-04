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
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.getmonero.i2p.zero.RouterWrapper;
import org.getmonero.i2p.zero.TunnelControl;

import static org.getmonero.i2p.zero.TunnelControl.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class Controller {

  private RouterWrapper routerWrapper;

  @FXML private BorderPane rootBorderPane;
  @FXML private Slider bandwidthSlider;
  @FXML private Label maxBandwidthLabel;
  @FXML private ImageView masterToggle;
  @FXML private AnchorPane bandwidthDisabledOverlay;
  @FXML private Tab bandwidthTab;
  @FXML private Tab tunnelsTab;
  @FXML private Tab eepSiteTab;
  @FXML private Tab helpTab;
  @FXML private Label statusLabel;
  @FXML private Button tunnelAddButton;
  @FXML private Button tunnelRemoveButton;
  @FXML private TableView<Tunnel> tunnelsTableView;
  @FXML private TableColumn typeCol;
  @FXML private TableColumn stateCol;
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
  @FXML private CheckBox eepSiteEnableCheckbox;
  @FXML private TextField eepSiteAddrField;
  @FXML private TextField eepSiteSecretKeyField;
  @FXML private Button eepSiteGenButton;
  @FXML private TextField eepSiteContentDirField;
  @FXML private Button eepSiteContentDirChooseButton;
  @FXML private TextField eepSiteLogsDirField;
  @FXML private Button eepSiteLogsDirChooseButton;
  @FXML private CheckBox eepSiteEnableLogsCheckbox;
  @FXML private CheckBox eepSiteAllowDirBrowsingCheckbox;
  @FXML private TextField eepSiteLocalPortField;


  DecimalFormat format2dp = new DecimalFormat("0.00");
  private boolean masterState = true;
  private final ObservableList<Tunnel> tunnelTableList = FXCollections.observableArrayList();
  private boolean eepSiteTabInitialized = false;
  
  private Stage getStage() {
    return (Stage) rootBorderPane.getScene().getWindow();
  }

  @FXML private void initialize() {

    DirectoryChooser directoryChooser = new DirectoryChooser();

    typeCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("type"));
    stateCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("state"));
    hostCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("host"));
    portCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("port"));
    i2PCol.setCellValueFactory(new PropertyValueFactory<Tunnel,String>("I2P"));

    DoubleBinding usedWidth = typeCol.widthProperty().add(stateCol.widthProperty()).add(hostCol.widthProperty()).add(portCol.widthProperty());
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
      getRouterWrapper().getTunnelControl().getTunnelList().removeTunnel(t);
      t.destroy(false);
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
        stage.setWidth(830);
      }
      else if(eepSiteTab.isSelected()) {
        stage.setWidth(830);
        stage.setHeight(500);

        if(!eepSiteTabInitialized) {
          EepSiteTunnel eepSiteTunnel = getEepSiteTunnel();
          eepSiteSecretKeyField.setText(eepSiteTunnel.keyPair.toString());
          eepSiteAddrField.setText("http://" + eepSiteTunnel.keyPair.b32Dest);
          eepSiteContentDirField.setText(eepSiteTunnel.contentDir);
          eepSiteLogsDirField.setText(eepSiteTunnel.logsDir);
          eepSiteEnableCheckbox.setSelected(eepSiteTunnel.enabled);
          eepSiteEnableLogsCheckbox.setSelected(eepSiteTunnel.enableLogs);
          eepSiteAllowDirBrowsingCheckbox.setSelected(eepSiteTunnel.allowDirectoryBrowsing);
          eepSiteLocalPortField.setText(eepSiteTunnel.port + "");
          eepSiteTabInitialized = true;

          // modifying eepSiteSecretKeyField/eepSiteLocalPortField will require the eepsite tunnel to be destroyed
          // and later recreated whenever the user is finished editing settings and ticks the enabled box again
          eepSiteSecretKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            eepSiteAddrField.setText("");
            String key = newValue;
            if(key!=null && !key.isEmpty()) {
              try {
                eepSiteAddrField.setText("http://" + new TunnelControl.KeyPair(key).b32Dest);
              }
              catch (Exception e2) {
                // ignore exception. user may be part way through entering string
              }
              eepSiteEnableCheckbox.setSelected(false);
            }
          });
          eepSiteGenButton.setOnAction(ev->{
            getEepSiteTunnel().keyPair = KeyPair.gen();
            eepSiteSecretKeyField.setText(getEepSiteTunnel().keyPair.toString());
          });
          eepSiteLocalPortField.textProperty().addListener((ov, oldValue, newValue)->{
            eepSiteEnableCheckbox.setSelected(false);
            getEepSiteTunnel().port = Integer.parseInt(newValue);
            save();
            eepSiteEnableCheckbox.setSelected(false);
          });

          eepSiteEnableCheckbox.selectedProperty().addListener((ov, oldValue, newValue)->{
            getEepSiteTunnel().enabled = newValue;
            if(oldValue!=newValue) {
              save();
              getRouterWrapper().getTunnelControl().getTunnelList().fireChangeEvent();
              if(getEepSiteTunnel().enabled) {
                getEepSiteTunnel().start();
              }
              else {
                getEepSiteTunnel().destroy(false);
              }
            }
          });


          // changing eepSiteContentDirChooseButton/eepSiteLogsDirChooseButton/eepSiteEnableLogsCheckbox/eepSiteAllowDirBrowsingCheckbox only requires a jetty restart
          eepSiteContentDirChooseButton.setOnAction(ev->{
            File selectedDirectory = directoryChooser.showDialog(getStage());
            if (selectedDirectory!=null) {
              eepSiteContentDirField.setText(selectedDirectory.getAbsolutePath());
              getEepSiteTunnel().contentDir = selectedDirectory.getAbsolutePath();
              save();
              if(getEepSiteTunnel().enabled) restartJetty();
            }
          });
          eepSiteLogsDirChooseButton.setOnAction(ev->{
            File selectedDirectory = directoryChooser.showDialog(getStage());
            if (selectedDirectory!=null) {
              eepSiteLogsDirField.setText(selectedDirectory.getAbsolutePath());
              getEepSiteTunnel().logsDir = selectedDirectory.getAbsolutePath();
              save();
              if(getEepSiteTunnel().enabled) restartJetty();
            }
          });
          eepSiteEnableLogsCheckbox.selectedProperty().addListener((ov, oldValue, newValue)->{
            getEepSiteTunnel().enableLogs = newValue;
            save();
            if(getEepSiteTunnel().enabled) restartJetty();
          });
          eepSiteAllowDirBrowsingCheckbox.selectedProperty().addListener((ov, oldValue, newValue)->{
            getEepSiteTunnel().allowDirectoryBrowsing = newValue;
            save();
            if(getEepSiteTunnel().enabled) restartJetty();
          });

        }
      }
    };
    Stream.of(bandwidthTab, tunnelsTab, eepSiteTab, helpTab).forEach(tab->tab.setOnSelectionChanged(tabSelectionEventHandler));

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
        routerWrapper.stop(false);
        tunnelTableList.clear();
        tunnelAddButton.setDisable(true);
      }
    });

    startRouter();

    new Thread(()->{
      while(getRouterWrapper()==null || getRouterWrapper().getTunnelControl()==null) {
        try { Thread.sleep(100); } catch (InterruptedException e) {}
      }
      var tunnelList = getRouterWrapper().getTunnelControl().getTunnelList();
      tunnelList.addChangeListener(tunnels->{
        tunnelTableList.clear();
        tunnels.stream().filter(Tunnel::getEnabled).forEach(tunnelTableList::add);
      });
    }).start();


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

            tunnelsTableView.refresh();

          });
        }
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
      }
    });
    bandwidthUpdateThread.start();

  }

  public RouterWrapper getRouterWrapper() {
    return routerWrapper;
  }

  private int getBandwidthLimitKBPerSec() {
    return (int)Math.round(bandwidthSlider.getValue()*1024d/8d);
  }

  private void startRouter() {

    var params = Gui.instance.getParameters().getNamed();

    Properties routerProperties = new Properties();
    routerProperties.put("i2np.inboundKBytesPerSecond", getBandwidthLimitKBPerSec());
    routerProperties.put("i2np.outboundKBytesPerSecond", getBandwidthLimitKBPerSec());
    routerProperties.put("router.sharePercentage", 80);
    params.entrySet().stream().forEach(e->routerProperties.put(e.getKey(), e.getValue()));
    routerWrapper = new RouterWrapper(routerProperties);
    routerWrapper.start();

  }

  private void restartJetty() {
    Optional<Tunnel> eepSiteTunnelOptional = getRouterWrapper().getTunnelControl().getTunnelList().getTunnelsCopyStream().filter(t->t.getType().equals("eepsite")).findFirst();
    EepSiteTunnel eepSiteTunnel = (EepSiteTunnel) eepSiteTunnelOptional.get();
    eepSiteTunnel.stopJetty();
    new Thread(()->eepSiteTunnel.startJetty()).start();
  }
  private void save() {
    getRouterWrapper().getTunnelControl().getTunnelList().save();
  }
  private EepSiteTunnel getEepSiteTunnel() {
    return getRouterWrapper().getTunnelControl().getEepSiteTunnel();
  }


}
