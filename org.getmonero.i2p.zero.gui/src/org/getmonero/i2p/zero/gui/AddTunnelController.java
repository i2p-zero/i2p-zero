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
  @FXML Pane httpProxyConfigPane;
  @FXML Pane socksProxyConfigPane;
  @FXML Button addButton;
  @FXML Button cancelButton;
  @FXML ToggleGroup tunnelType;
  @FXML RadioButton clientTunnelRadioButton;
  @FXML RadioButton serverTunnelRadioButton;
  @FXML RadioButton httpProxyRadioButton;
  @FXML RadioButton socksProxyRadioButton;
  @FXML TextField clientDestAddrField;
  @FXML TextField clientPortField;
  @FXML TextField serverHostField;
  @FXML TextField serverPortField;
  @FXML TextField serverKeyField;
  @FXML TextField serverAddrField;
  @FXML TextField socksPortField;
  @FXML TextField httpProxyPortField;


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
    else if(tunnelType.getSelectedToggle().equals(httpProxyRadioButton)) {
      addButton.setDisable(Stream.of(httpProxyPortField).anyMatch(f->f.getText().isBlank()));
    }
  }

  @FXML
  private void initialize() {

    serverTunnelConfigPane.setVisible(false);
    socksProxyConfigPane.setVisible(false);
    httpProxyConfigPane.setVisible(false);

    addButton.setOnAction(ev->{
      try {
        var controller = Gui.instance.getController();
        var tunnelControl = controller.getRouterWrapper().getTunnelControl();
        var tunnelList = tunnelControl.getTunnelList();
        if (tunnelType.getSelectedToggle().equals(clientTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ClientTunnel(clientDestAddrField.getText(), Integer.parseInt(clientPortField.getText())).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(serverTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ServerTunnel(serverHostField.getText(), Integer.parseInt(serverPortField.getText()), new TunnelControl.KeyPair(serverKeyField.getText()), tunnelControl.getTunnelControlTempDir()).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(socksProxyRadioButton)) {
          Tunnel t = new TunnelControl.SocksTunnel(Integer.parseInt(socksPortField.getText())).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(httpProxyRadioButton)) {
          Tunnel t = new TunnelControl.HttpClientTunnel(Integer.parseInt(httpProxyPortField.getText())).start();
          tunnelList.addTunnel(t);
        }
        clientTunnelConfigPane.getScene().getWindow().hide();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });

    TextField[] allTextFields = new TextField[] {clientDestAddrField, clientPortField, serverHostField, serverPortField, serverKeyField, serverAddrField, socksPortField, httpProxyPortField};
    for(TextField f : allTextFields) {
      f.textProperty().addListener((observable, oldValue, newValue) -> updateAddButtonState());
    }


    cancelButton.setOnAction(e->{
      clientTunnelConfigPane.getScene().getWindow().hide();
    });

    tunnelType.selectedToggleProperty().addListener((ov, oldToggle, newToggle)-> {
      try {
        clientTunnelConfigPane.setVisible(false);
        serverTunnelConfigPane.setVisible(false);
        httpProxyConfigPane.setVisible(false);
        socksProxyConfigPane.setVisible(false);
        if (newToggle.equals(clientTunnelRadioButton)) clientTunnelConfigPane.setVisible(true);
        if (newToggle.equals(serverTunnelRadioButton)) {
          var keyPair = TunnelControl.KeyPair.gen();
          serverKeyField.setText(keyPair.toString());
          serverAddrField.setText(keyPair.b32Dest);
          serverTunnelConfigPane.setVisible(true);
        }
        if (newToggle.equals(httpProxyRadioButton)) httpProxyConfigPane.setVisible(true);
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
          serverAddrField.setText(new TunnelControl.KeyPair(key).b32Dest);
        }
        catch (Exception e) {
          // ignore exception. user may be part way through entering string
        }
      }
    });

  }

}
