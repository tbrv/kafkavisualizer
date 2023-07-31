package kafkavisualizer.navigator.actions;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import kafkavisualizer.App;
import kafkavisualizer.Utils;
import kafkavisualizer.dialog.DialogController;
import kafkavisualizer.navigator.ClusterPane;
import kafkavisualizer.navigator.nodes.ClusterNode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class EditClusterAction extends AbstractAction {

    public EditClusterAction() {
        super("Edit Cluster", new FlatSVGIcon("icons/edit.svg"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var controller = App.getAppController().getNavigatorController();
        var selection = controller.getNavigatorPane().getTree().getSelectionPath();
        if (selection == null) {
            return;
        }
        var clusterNode = (ClusterNode) selection.getLastPathComponent();
        var cluster = clusterNode.getCluster();

        var clusterPane = new ClusterPane();
        var dialogController = new DialogController(App.contentPane(), clusterPane, "Edit Cluster");
        clusterPane.getNameTextField().setText(cluster.getName());
        clusterPane.getServersTextField().setText(cluster.getServers());
        clusterPane.getAesKeysTextField().setText(String.join(",", cluster.getAesKeys()));

        dialogController.addOKAction(e1 -> {
            var name = clusterPane.getNameTextField().getText();
            var servers = clusterPane.getServersTextField().getText();
            var aesKeys = clusterPane.getAesKeysTextField().getText();

            if (name == null || name.trim().length() == 0
                    || servers == null || servers.trim().length() == 0) {
                return;
            }

            if (!servers.equals(cluster.getServers())) {
                var confirmed = Utils.showConfirmation(App.contentPane(), "This will stop and clear all consumers, continue?");
                if (!confirmed) {
                    return;
                }
                App.getAppController().getDetailsController().stopConsumers(cluster);
                App.getAppController().getDetailsController().clearConsumers(cluster);
            }

            List<String> aesKeysSplit = new ArrayList<>();
            if (aesKeys != null) {
                aesKeysSplit = Arrays.stream(aesKeys.trim().split(",")).map(String::trim).collect(toList());
            }

            cluster.setName(name);
            cluster.setServers(servers);
            cluster.setAesKeys(aesKeysSplit);

            try {
                controller.getNavigatorModel().save();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(controller.getNavigatorPane(), "Cannot save.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            //controller.updateTreeFromModel();
            dialogController.closeDialog();
        });
        dialogController.addCancelAction(e1 -> dialogController.closeDialog());
        dialogController.showDialog();
    }
}
