package org.getmonero.i2p.zero.gui;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.getmonero.i2p.zero.RouterWrapper;
import org.getmonero.i2p.zero.TunnelControl;
import org.getmonero.i2p.zero.UpdateCheck;

import static org.getmonero.i2p.zero.TunnelControl.*;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Controller {

  private RouterWrapper routerWrapper;

  @FXML private BorderPane rootBorderPane;
  @FXML private Slider bandwidthSlider;
  @FXML private Label maxBandwidthLabel;
  @FXML private ImageView masterToggle;
  @FXML private AnchorPane bandwidthDisabledOverlay;
  @FXML private TabPane tabPane;
  @FXML private Tab bandwidthTab;
  @FXML private Tab tunnelsTab;
  @FXML private Tab eepSiteTab;
  @FXML private Tab helpTab;
  @FXML private Label statusLabel;
  @FXML private Button tunnelAddButton;
  @FXML private Button tunnelRemoveButton;
  @FXML private Button tunnelEditButton;
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
  @FXML private TextArea helpTextArea;


  DecimalFormat format2dp = new DecimalFormat("0.00");
  private boolean masterState = true;
  private boolean shuttingDown = false;
  private final ObservableList<Tunnel> tunnelTableList = FXCollections.observableArrayList();
  private boolean eepSiteTabInitialized = false;
  
  private Stage getStage() {
    return (Stage) rootBorderPane.getScene().getWindow();
  }

  public static class DialogRefs {
    public Scene scene;
    public Stage stage;
    public AddTunnelController controller;
    public DialogRefs(Scene scene, Stage stage, AddTunnelController controller) {
      this.scene = scene;
      this.stage = stage;
      this.controller = controller;
    }
  }

  @FXML private void initialize() {

    // set up copy-cell-to-clipboard functionality
    tunnelsTableView.setOnKeyPressed(new TableKeyEventHandler());

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
        tunnelRemoveButton.setVisible(false);
        tunnelEditButton.setVisible(false);
      }
      else {
        tunnelRemoveButton.setVisible(true);
        tunnelEditButton.setVisible(true);
      }
    });

    tunnelRemoveButton.setOnAction(e->{
      Tunnel t = tunnelsTableView.getSelectionModel().getSelectedItem();
      getRouterWrapper().getTunnelControl().getTunnelList().removeTunnel(t);
      t.destroy(false);
      tunnelRemoveButton.setDisable(true);
    });

    Supplier<DialogRefs> showAddTunnelDialog = ()->{
      try {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(getStage());
        dialogStage.setResizable(false);
        dialogStage.setTitle("New tunnel");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("addTunnel.fxml"));
        Scene dialogScene = new Scene(loader.load());
        dialogScene.getStylesheets().add("org/getmonero/i2p/zero/gui/gui.css");
        dialogStage.setScene(dialogScene);
        return new DialogRefs(dialogScene, dialogStage, loader.getController());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    tunnelAddButton.setOnAction(event->{
      DialogRefs dialogRefs = showAddTunnelDialog.get();
      dialogRefs.stage.show();
    });
    tunnelEditButton.setOnAction(event->{
      Tunnel existingTunnel = tunnelsTableView.getSelectionModel().getSelectedItem();
      if(existingTunnel.getType().equals("eepsite")) {
        tabPane.getSelectionModel().select(eepSiteTab);
      }
      else {
        DialogRefs dialogRefs = showAddTunnelDialog.get();
        dialogRefs.stage.setTitle("Edit tunnel");
        dialogRefs.controller.setExistingTunnel(existingTunnel);
        dialogRefs.controller.setExistingTunnelDestroyer(()->{
          getRouterWrapper().getTunnelControl().getTunnelList().removeTunnel(existingTunnel);
          existingTunnel.destroy(false);
        });
        dialogRefs.stage.show();
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
            eepSiteEnableCheckbox.setSelected(false);
            eepSiteAddrField.setText("");
            String key = newValue;
            if(key!=null && !key.isEmpty()) {
              try {
                KeyPair keyPair =  new TunnelControl.KeyPair(key);
                eepSiteAddrField.setText("http://" + keyPair.b32Dest);
                getEepSiteTunnel().dest = keyPair.b32Dest;
                getEepSiteTunnel().keyPair = keyPair;
                save();
              }
              catch (Exception e2) {
                // ignore exception. user may be part way through entering string
              }
            }
          });
          eepSiteGenButton.setOnAction(ev->{
            getEepSiteTunnel().keyPair = KeyPair.gen();
            save();
            eepSiteSecretKeyField.setText(getEepSiteTunnel().keyPair.toString());
          });
          eepSiteLocalPortField.textProperty().addListener((ov, oldValue, newValue)->{
            eepSiteEnableCheckbox.setSelected(false);
            getEepSiteTunnel().port = Integer.parseInt(newValue);
            save();
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
      if(shuttingDown) return;
      masterState = !masterState;
      bandwidthDisabledOverlay.setVisible(!masterState);
      if(masterState) {
        masterToggle.setImage(new Image("org/getmonero/i2p/zero/gui/toggle-on.png"));
        statusLabel.setVisible(true);
        routerWrapper.start(()->{});
        listenForTunnelChanges();
        tunnelAddButton.setDisable(false);
      }
      else {
        shuttingDown = true;
        masterToggle.setImage(new Image("org/getmonero/i2p/zero/gui/toggle-off.png"));
        tunnelTableList.clear();
        tunnelAddButton.setDisable(true);
        statusLabel.setText("Shutting down...");
        routerWrapper.stop(true);
      }
    });

    startRouter();

    bandwidthSlider.setValue(getRouterWrapper().loadBandwidthLimitKBps()*8d/1024d);

    listenForTunnelChanges();

    var bandwidthUpdateThread = new Thread(()->{
      while(!Gui.instance.isStopping()) {
        if (routerWrapper.isStarted()) {
          Platform.runLater(() -> {
            bandwidthIn1s.setText(format2dp.format(routerWrapper.get1sRateInKBps()) + " KBps");
            bandwidthIn5m.setText(format2dp.format(routerWrapper.get5mRateInKBps()) + " KBps");
            bandwidthInAll.setText(format2dp.format(routerWrapper.getAvgRateInKBps()) + " KBps");
            totalTransferredIn.setText(formatTransferAmount(routerWrapper.getTotalInMB())+"   ");
            bandwidthOut1s.setText(format2dp.format(routerWrapper.get1sRateOutKBps()) + " KBps");
            bandwidthOut5m.setText(format2dp.format(routerWrapper.get5mRateOutKBps()) + " KBps");
            bandwidthOutAll.setText(format2dp.format(routerWrapper.getAvgRateOutKBps()) + " KBps");
            totalTransferredOut.setText(formatTransferAmount(routerWrapper.getTotalOutMB())+"   ");

            statusLabel.setText("Status: " + routerWrapper.getReachability().getMessage());

            tunnelsTableView.refresh();

          });
        }
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
      }
    });
    bandwidthUpdateThread.start();

  }

  private String formatTransferAmount(double amt) {
    String unit = "MB";
    if(amt>=1000) { amt/= 1000d; unit = "GB"; }
    if(amt>=1000) { amt/= 1000d; unit = "TB"; }
    if(amt>=1000) { amt/= 1000d; unit = "PB"; }
    return format2dp.format(amt) + " " + unit;
  }

  private void listenForTunnelChanges() {
    new Thread(()->{
      while(getRouterWrapper()==null || getRouterWrapper().getTunnelControl()==null) {
        try { Thread.sleep(100); } catch (InterruptedException e) {}
      }
      var tunnelList = getRouterWrapper().getTunnelControl().getTunnelList();
      tunnelList.addChangeListener(tunnels->{
        tunnelTableList.clear();
        tunnels.stream().filter(Tunnel::getEnabled).forEach(tunnelTableList::add);
      });
      getRouterWrapper().getTunnelControl().getTunnelList().fireChangeEvent();
    }).start();
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
    routerProperties.put("router.sharePercentage", 80);
    params.entrySet().stream().forEach(e->routerProperties.put(e.getKey(), e.getValue()));
    routerWrapper = new RouterWrapper(routerProperties, ()-> {
      Platform.runLater(()->{
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update available");
        alert.setHeaderText(null);

        FlowPane fp = new FlowPane();
        String updateUrl = "https://github.com/i2p-zero/i2p-zero";

        var link = new Hyperlink(updateUrl);
        TextFlow textFlow = new TextFlow(
          new Text("A new version of I2P-zero is available at"),
          link,
          new Text("\nPlease keep your software up-to-date, as it will enhance your privacy and keep you safe from vulnerabilities")
        );
        textFlow.setPrefWidth(300);
        link.setOnAction((ActionEvent event) -> {
          try {
            Desktop.getDesktop().browse(new URI(updateUrl));
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        });
        fp.getChildren().addAll(textFlow);
        alert.getDialogPane().contentProperty().set(fp);
        alert.show();
      });
    });
    routerWrapper.start(()->{
      helpTextArea.setText("You are running I2P-zero version " + UpdateCheck.currentVersion + "\n\n"
        + "For best performance, please open port " + routerWrapper.routerExternalPort + " on your firewall for incoming UDP and TCP connections. This port has been randomly assigned to you. For privacy reasons, please do not share this port with others.\n\n"
        + helpTextArea.getText());
    });

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

  public static class TableKeyEventHandler implements EventHandler<KeyEvent> {
    KeyCodeCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    KeyCodeCombination cmdD = new KeyCodeCombination(KeyCode.C, KeyCombination.META_ANY);
    public void handle(final KeyEvent keyEvent) {
      if (ctrlC.match(keyEvent) || cmdD.match(keyEvent)) {
        if (keyEvent.getSource() instanceof TableView) {
          copySelectionToClipboard((TableView<?>) keyEvent.getSource());
          keyEvent.consume();
        }
      }
    }
  }

  public static void copySelectionToClipboard(TableView<?> table) {
    var cellPositions = table.getSelectionModel().getSelectedCells();
    if(cellPositions.size()>0) {
      var cellPosition = cellPositions.get(0);
      var cell = table.getColumns().get(cellPosition.getColumn()).getCellData(cellPosition.getRow());
      if(cell!=null) {
        var clipboardContent = new ClipboardContent();
        clipboardContent.putString(cell.toString());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
      }
    }
  }

}
