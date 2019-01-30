package org.getmonero.i2p.zero.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.getmonero.i2p.zero.TunnelControl;

import java.util.stream.Stream;
import static org.getmonero.i2p.zero.TunnelControl.Tunnel;

public class AddTunnelController {

  @FXML Pane clientTunnelConfigPane;
  @FXML Pane serverTunnelConfigPane;
  @FXML Pane socksProxyConfigPane;
  @FXML Button addButton;
  @FXML Button cancelButton;
  @FXML ToggleGroup tunnelType;
  @FXML RadioButton clientTunnelRadioButton;
  @FXML RadioButton serverTunnelRadioButton;
  @FXML RadioButton socksProxyRadioButton;
  @FXML TextField clientDestAddrField;
  @FXML TextField clientPortField;
  @FXML TextField serverHostField;
  @FXML TextField serverPortField;
  @FXML TextField serverKeyField;
  @FXML TextField serverAddrField;
  @FXML TextField socksPortField;


  private void updateAddButtonState() {
    if(tunnelType.getSelectedToggle().equals(clientTunnelRadioButton)) {
      addButton.setDisable(Stream.of(clientDestAddrField, clientPortField).anyMatch(f->f.getText().isBlank()));
    }
    else if(tunnelType.getSelectedToggle().equals(serverTunnelRadioButton)) {
      addButton.setDisable(Stream.of(serverHostField, serverPortField, serverKeyField, serverAddrField).anyMatch(f->f.getText().isBlank()));
    }
    else if(tunnelType.getSelectedToggle().equals(socksProxyRadioButton)) {
      addButton.setDisable(Stream.of(socksPortField).anyMatch(f->f.getText().isBlank()));
    }
  }

  @FXML
  private void initialize() {

    serverTunnelConfigPane.setVisible(false);
    socksProxyConfigPane.setVisible(false);

    addButton.setOnAction(ev->{
      try {
        var controller = Gui.instance.getController();
        var tunnelControl = controller.getRouterWrapper().getTunnelControl();
        var tunnels = tunnelControl.getTunnels();
        if (tunnelType.getSelectedToggle().equals(clientTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ClientTunnel(clientDestAddrField.getText(), Integer.parseInt(clientPortField.getText()));
          tunnels.add(t);
          controller.tunnelTableList.add(t);
        } else if (tunnelType.getSelectedToggle().equals(serverTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ServerTunnel(serverHostField.getText(), Integer.parseInt(serverPortField.getText()), new TunnelControl.KeyPair(serverKeyField.getText()), tunnelControl.getTunnelControlTempDir());
          tunnels.add(t);
          controller.tunnelTableList.add(t);
        } else if (tunnelType.getSelectedToggle().equals(socksProxyRadioButton)) {
          Tunnel t = new TunnelControl.SocksTunnel(Integer.parseInt(socksPortField.getText()));
          tunnels.add(t);
          controller.tunnelTableList.add(t);
        }
        clientTunnelConfigPane.getScene().getWindow().hide();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });

    TextField[] allTextFields = new TextField[] {clientDestAddrField, clientPortField, serverHostField, serverPortField, serverKeyField, serverAddrField, socksPortField};
    for(TextField f : allTextFields) {
      f.textProperty().addListener((observable, oldValue, newValue) -> updateAddButtonState());
    }


    cancelButton.setOnAction(e->{
      clientTunnelConfigPane.getScene().getWindow().hide();
    });

    tunnelType.selectedToggleProperty().addListener((ov, oldToggle, newToggle)-> {
      try {
        var tunnelControl = Gui.instance.getController().getRouterWrapper().getTunnelControl();
        clientTunnelConfigPane.setVisible(false);
        serverTunnelConfigPane.setVisible(false);
        socksProxyConfigPane.setVisible(false);
        if (newToggle.equals(clientTunnelRadioButton)) clientTunnelConfigPane.setVisible(true);
        if (newToggle.equals(serverTunnelRadioButton)) {
          var keyPair = TunnelControl.KeyPair.gen();
          serverKeyField.setText(keyPair.seckey + "," + keyPair.pubkey);
          serverAddrField.setText(keyPair.b32Dest);
          serverTunnelConfigPane.setVisible(true);
        }
        if (newToggle.equals(socksProxyRadioButton)) socksProxyConfigPane.setVisible(true);
        updateAddButtonState();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    serverAddrField.setEditable(false);

    serverKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
      serverAddrField.setText("");
      String key = newValue;
      if(key!=null && !key.isEmpty()) {
        try {
          TunnelControl.KeyPair keyPair = new TunnelControl.KeyPair(key);
          serverAddrField.setText(keyPair.b32Dest);
        }
        catch (Exception e) {
          // ignore exception. user may be part way through entering string
        }

      }
    });

  }

}
