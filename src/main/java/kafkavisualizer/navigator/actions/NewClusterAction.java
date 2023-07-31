package kafkavisualizer.navigator.actions;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import kafkavisualizer.App;
import kafkavisualizer.KafkaClient;
import kafkavisualizer.Utils;
import kafkavisualizer.dialog.DialogAction;
import kafkavisualizer.dialog.DialogController;
import kafkavisualizer.models.Cluster;
import kafkavisualizer.navigator.ClusterPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class NewClusterAction extends AbstractAction {

    public NewClusterAction() {
        super("New Cluster", new FlatSVGIcon("icons/server.svg"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var navigatorController = App.getAppController().getNavigatorController();
        var clusterPane = new ClusterPane();
        var dialogController = new DialogController(App.contentPane(), clusterPane, "Add Cluster");

        var testAction = new DialogAction("Test", null);
        testAction.setListener(e1 -> {
            testAction.setEnabled(false);
            var servers = clusterPane.getServersTextField().getText();
            KafkaClient.getClusterId(servers, (s, throwable) -> {
                EventQueue.invokeLater(() -> {
                    testAction.setEnabled(true);
                    if (throwable != null) {
                        Utils.showError(App.contentPane(), throwable.getMessage());
                    } else {
                        Utils.showInfo(App.contentPane(), "Successful");
                    }
                });
            });
        });
        dialogController.addAction(testAction);

        dialogController.addOKAction(e1 -> {
            var name = clusterPane.getNameTextField().getText();
            var servers = clusterPane.getServersTextField().getText();
            var aesKeys = clusterPane.getAesKeysTextField().getText();

            if (name == null || name.trim().length() == 0
                    || servers == null || servers.trim().length() == 0
                    || aesKeys == null || aesKeys.trim().length() == 0) {
                return;
            }

            var aesKeysSplit = Arrays.stream(aesKeys.split(",")).map(String::trim).collect(toList());

            var cluster = new Cluster(name, servers, aesKeysSplit);
            navigatorController.addCluster(cluster);
            dialogController.closeDialog();
        });
        dialogController.addCancelAction(e1 -> dialogController.closeDialog());
        dialogController.showDialog();
    }
}
