package kafkavisualizer.details.consumer;

import kafkavisualizer.Utils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ConsumerEventPane extends JPanel {
    private final JPanel valuePane;
    private final JPanel keyPane;
    private final JPanel headersPane;
    private final RSyntaxTextArea valueTextArea;
    private final JCheckBox valueTextAreaWordWrapCheckBox;
    private final JCheckBox valueTextAreaFormatCheckBox;
    private final JTextArea keyTextArea;
    private final JCheckBox keyTextAreaWordWrapCheckBox;
    private final JCheckBox keyTextAreaFormatCheckBox;
    private final JTable headersTable;

    public ConsumerEventPane() {
        valuePane = new JPanel();
        valuePane.setLayout(new BorderLayout());
        var valueToolbar = new JToolBar();
        valueTextAreaWordWrapCheckBox = new JCheckBox("Word Wrap");
        valueTextAreaFormatCheckBox = new JCheckBox("Format");
        valueToolbar.setBorder(new EmptyBorder(8, 0, 8, 0));
        valueToolbar.add(valueTextAreaWordWrapCheckBox);
        valueToolbar.add(valueTextAreaFormatCheckBox);
        valuePane.add(valueToolbar, BorderLayout.NORTH);
        valueTextArea = new RSyntaxTextArea();
        valueTextArea.setLineWrap(false);
        valueTextArea.setFont(Utils.getMonoFont());
        valueTextArea.setEditable(false);
        valueTextArea.setCodeFoldingEnabled(true);
        valuePane.add(new JScrollPane(valueTextArea), BorderLayout.CENTER);

        keyPane = new JPanel();
        keyPane.setLayout(new BorderLayout());
        var keyToolbar = new JToolBar();
        keyTextAreaWordWrapCheckBox = new JCheckBox("Word Wrap");
        keyTextAreaFormatCheckBox = new JCheckBox("Format");
        keyToolbar.setBorder(new EmptyBorder(8, 0, 8, 0));
        keyToolbar.add(keyTextAreaWordWrapCheckBox);
        keyToolbar.add(keyTextAreaFormatCheckBox);
        keyPane.add(keyToolbar, BorderLayout.NORTH);
        keyTextArea = new JTextArea();
        keyTextArea.setLineWrap(false);
        keyTextArea.setFont(Utils.getMonoFont());
        keyTextArea.setEditable(false);
        keyPane.add(new JScrollPane(keyTextArea), BorderLayout.CENTER);

        headersPane = new JPanel();
        headersPane.setLayout(new BorderLayout());

        headersTable = new JTable();
        headersTable.setShowGrid(true);
        headersPane.add(new JScrollPane(headersTable), BorderLayout.CENTER);

        var tabbedPane = new JTabbedPane();
        tabbedPane.add("Value", valuePane);
        tabbedPane.add("Key", keyPane);
        tabbedPane.add("Headers", headersPane);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    public JPanel getValuePane() {
        return valuePane;
    }

    public JPanel getKeyPane() {
        return keyPane;
    }

    public JPanel getHeadersPane() {
        return headersPane;
    }

    public RSyntaxTextArea getValueTextArea() {
        return valueTextArea;
    }

    public JTextArea getKeyTextArea() {
        return keyTextArea;
    }

    public JTable getHeadersTable() {
        return headersTable;
    }

    public JCheckBox getValueTextAreaWordWrapCheckBox() {
        return valueTextAreaWordWrapCheckBox;
    }

    public JCheckBox getValueTextAreaFormatCheckBox() {
        return valueTextAreaFormatCheckBox;
    }

    public JCheckBox getKeyTextAreaWordWrapCheckBox() {
        return keyTextAreaWordWrapCheckBox;
    }

    public JCheckBox getKeyTextAreaFormatCheckBox() {
        return keyTextAreaFormatCheckBox;
    }
}
