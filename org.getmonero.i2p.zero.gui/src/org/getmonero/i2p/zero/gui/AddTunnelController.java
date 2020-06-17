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
  @FXML Button okButton;
  @FXML Button cancelButton;
  @FXML Label tunnelAddEditLabel;
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

  public AddTunnelController() {
    super();
  }

  Runnable existingTunnelDestroyer;
  void setExistingTunnelDestroyer(Runnable r) {
    existingTunnelDestroyer = r;
  }

  void setExistingTunnel(Tunnel t) {
    switch(t.getType()) {
      case "server":
        serverTunnelRadioButton.fire();
        TunnelControl.ServerTunnel serverTunnel = (TunnelControl.ServerTunnel) t;
        serverHostField.setText(serverTunnel.host);
        serverPortField.setText(serverTunnel.port + "");
        serverKeyField.setText(serverTunnel.keyPair.toString());
        break;
      case "client":
        clientTunnelRadioButton.fire();
        TunnelControl.ClientTunnel clientTunnel = (TunnelControl.ClientTunnel) t;
        clientDestAddrField.setText(clientTunnel.dest);
        clientPortField.setText(clientTunnel.port + "");
        break;
      case "socks":
        socksProxyRadioButton.fire();
        TunnelControl.SocksTunnel socksTunnel = (TunnelControl.SocksTunnel) t;
        socksPortField.setText(socksTunnel.port + "");
        break;
      case "http":
        httpProxyRadioButton.fire();
        TunnelControl.HttpClientTunnel httpClientTunnel = (TunnelControl.HttpClientTunnel) t;
        socksPortField.setText(httpClientTunnel.port + "");
        break;
    }
  }

  private void updateOkButtonState() {
    if(tunnelType.getSelectedToggle().equals(clientTunnelRadioButton)) {
      okButton.setDisable(Stream.of(clientDestAddrField, clientPortField).anyMatch(f->f.getText().isBlank()));
    }
    else if(tunnelType.getSelectedToggle().equals(serverTunnelRadioButton)) {
      okButton.setDisable(Stream.of(serverHostField, serverPortField, serverKeyField, serverAddrField).anyMatch(f->f.getText().isBlank()));
    }
    else if(tunnelType.getSelectedToggle().equals(socksProxyRadioButton)) {
      okButton.setDisable(Stream.of(socksPortField).anyMatch(f->f.getText().isBlank()));
    }
    else if(tunnelType.getSelectedToggle().equals(httpProxyRadioButton)) {
      okButton.setDisable(Stream.of(httpProxyPortField).anyMatch(f->f.getText().isBlank()));
    }
  }

  @FXML
  private void initialize() {

    serverTunnelConfigPane.setVisible(false);
    socksProxyConfigPane.setVisible(false);
    httpProxyConfigPane.setVisible(false);

    okButton.setOnAction(ev->{
      try {
        if(existingTunnelDestroyer!=null) existingTunnelDestroyer.run();
        var controller = Gui.instance.getController();
        var tunnelControl = controller.getRouterWrapper().getTunnelControl();
        var tunnelList = tunnelControl.getTunnelList();
        if (tunnelType.getSelectedToggle().equals(clientTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ClientTunnel(clientDestAddrField.getText(), Integer.parseInt(clientPortField.getText()), controller.getRouterWrapper()).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(serverTunnelRadioButton)) {
          Tunnel t = new TunnelControl.ServerTunnel(serverHostField.getText(), Integer.parseInt(serverPortField.getText()), new TunnelControl.KeyPair(serverKeyField.getText()), tunnelControl.getTunnelControlTempDir(), controller.getRouterWrapper()).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(socksProxyRadioButton)) {
          Tunnel t = new TunnelControl.SocksTunnel(Integer.parseInt(socksPortField.getText()), controller.getRouterWrapper()).start();
          tunnelList.addTunnel(t);
        } else if (tunnelType.getSelectedToggle().equals(httpProxyRadioButton)) {
          Tunnel t = new TunnelControl.HttpClientTunnel(Integer.parseInt(httpProxyPortField.getText()), controller.getRouterWrapper()).start();
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
      f.textProperty().addListener((observable, oldValue, newValue) -> updateOkButtonState());
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
        updateOkButtonState();
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
