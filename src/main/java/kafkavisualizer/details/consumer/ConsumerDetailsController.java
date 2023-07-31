package kafkavisualizer.details.consumer;

import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;
import kafkavisualizer.App;
import kafkavisualizer.Utils;
import kafkavisualizer.details.consumer.actions.ClearAction;
import kafkavisualizer.details.consumer.actions.StartAction;
import kafkavisualizer.details.consumer.actions.StopAction;
import kafkavisualizer.models.Cluster;
import kafkavisualizer.models.Consumer;
import kafkavisualizer.models.HeaderRow;
import kafkavisualizer.navigator.actions.EditConsumerAction;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class ConsumerDetailsController {

    private static final String CIPHER_ALGORITHM_MODE_AND_PADDING = "AES/GCM/NoPadding";
    private static final String PLAINTEXT_KEY_ID = "plaintext";

    public static class HeadersTableModel extends AbstractTableModel {

        private static final String[] COL_NAMES = {"Key", "Value"};
        private final List<HeaderRow> headerRows = new ArrayList<>();

        @Override
        public String getColumnName(int column) {
            return COL_NAMES[column];
        }

        @Override
        public int getRowCount() {
            return headerRows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            var row = headerRows.get(rowIndex);
            if (columnIndex == 0) {
                return row.getKey();
            } else if (columnIndex == 1) {
                return row.getValue();
            }
            return null;
        }

        public List<HeaderRow> getHeaders() {
            return headerRows;
        }
    }

    public class ConsumerTableSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateConsumerPane();
        }
    }

    private final ConsumerDetailsPane pane;
    private final StartAction startAction;
    private final StopAction stopAction;
    private final ClearAction clearAction;
    private final EditConsumerAction editConsumerAction;
    private final ConsumerTableModel consumerTableModel;
    private final Consumer consumer;
    private final Cluster cluster;
    private final ConsumerModel consumerModel;
    private final HeadersTableModel headersTableModel;
    private final ConsumerTableSelectionListener consumerTableSelectionListener;

    public ConsumerDetailsController(Cluster cluster, Consumer consumer) {
        this.cluster = cluster;
        this.consumer = consumer;
        consumerModel = new ConsumerModel(cluster, consumer);
        consumerTableSelectionListener = new ConsumerTableSelectionListener();
        pane = new ConsumerDetailsPane();

        startAction = new StartAction(this);
        stopAction = new StopAction(this);
        stopAction.setEnabled(false);
        clearAction = new ClearAction(this);
        editConsumerAction = new EditConsumerAction();
        pane.getStartButton().setAction(startAction);
        pane.getStopButton().setAction(stopAction);
        pane.getEditButton().setAction(editConsumerAction);
        pane.getClearButton().setAction(clearAction);

        pane.getSearchTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                consumerTableModel.setSearchText(pane.getSearchTextField().getText());
                consumerTableModel.fireTableDataChanged();
            }
        });

        consumerTableModel = new ConsumerTableModel();
        pane.getTable().setModel(consumerTableModel);
        pane.getTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pane.getTable().getSelectionModel().addListSelectionListener(consumerTableSelectionListener);

        var columnModel = pane.getTable().getColumnModel();

        columnModel.getColumn(0).setPreferredWidth(180); // timestamp
        columnModel.getColumn(0).setMaxWidth(180);

        columnModel.getColumn(1).setPreferredWidth(150); // topic
        columnModel.getColumn(1).setMaxWidth(200);

        columnModel.getColumn(2).setPreferredWidth(100); // partition
        columnModel.getColumn(2).setMaxWidth(100);

        columnModel.getColumn(3).setPreferredWidth(100); // offset
        columnModel.getColumn(3).setMaxWidth(100);

        columnModel.getColumn(4).setPreferredWidth(200); // key

        columnModel.getColumn(5).setPreferredWidth(300); // value

        pane.getConsumerEventPane().getValueTextAreaWordWrapCheckBox().addActionListener(e -> {
            var selected = pane.getConsumerEventPane().getValueTextAreaWordWrapCheckBox().isSelected();
            pane.getConsumerEventPane().getValueTextArea().setLineWrap(selected);
            pane.getConsumerEventPane().getValueTextArea().setWrapStyleWord(selected);
        });

        pane.getConsumerEventPane().getValueTextAreaFormatCheckBox().addActionListener(e -> updateConsumerPane());
        pane.getConsumerEventPane().getKeyTextAreaFormatCheckBox().addActionListener(e -> updateConsumerPane());

        pane.getConsumerEventPane().getKeyTextAreaWordWrapCheckBox().addActionListener(e -> {
            var selected = pane.getConsumerEventPane().getKeyTextAreaWordWrapCheckBox().isSelected();
            pane.getConsumerEventPane().getKeyTextArea().setLineWrap(selected);
            pane.getConsumerEventPane().getKeyTextArea().setWrapStyleWord(selected);
        });

        headersTableModel = new HeadersTableModel();
        pane.getConsumerEventPane().getHeadersTable().setModel(headersTableModel);
    }

    public void updateConsumerPane() {
        if (pane.getTable().getSelectedRow() != -1) {
            var selectedRow = pane.getTable().getSelectedRow();
            var record = consumerTableModel.getSelectedRecord(selectedRow);

            String value = maybeDecryptValue(record);
            if (pane.getConsumerEventPane().getValueTextAreaFormatCheckBox().isSelected()) {
                switch (consumer.getValueFormat()) {
                    case JSON:
                        value = Utils.beautifyJSON(value);
                        break;
                    case XML:
                        value = Utils.beautifyXML(value);
                        break;
                    case PLAIN_TEXT:
                    default:
                        break;
                }
            }
            pane.getConsumerEventPane().getValueTextArea().setText(value);
            pane.getConsumerEventPane().getValueTextArea().setCaretPosition(0);

            var key = record.key();
            if (pane.getConsumerEventPane().getKeyTextAreaFormatCheckBox().isSelected()) {
                switch (consumer.getKeyFormat()) {
                    case JSON:
                        key = Utils.beautifyJSON(key);
                        break;
                    case XML:
                        key = Utils.beautifyXML(key);
                        break;
                    case PLAIN_TEXT:
                    default:
                        break;
                }
            }
            pane.getConsumerEventPane().getKeyTextArea().setText(key);
            pane.getConsumerEventPane().getKeyTextArea().setCaretPosition(0);

            headersTableModel.getHeaders().clear();
            for (var header : record.headers()) {
                var v = header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
                headersTableModel.getHeaders().add(new HeaderRow(header.key(), v));
            }
            headersTableModel.fireTableDataChanged();
        } else {
            pane.getConsumerEventPane().getValueTextArea().setText("");
            pane.getConsumerEventPane().getKeyTextArea().setText("");
            headersTableModel.getHeaders().clear();
        }
    }

    private String maybeDecryptValue(ConsumerRecord<String, byte[]> kafkaRecord) {
        return getEncryptionMetadata(kafkaRecord)
                .flatMap(encryptionMetadata -> decrypt(kafkaRecord.value(), encryptionMetadata))
                .map(plaintextBytes -> new String(plaintextBytes, StandardCharsets.UTF_8))
                .orElse(new String(kafkaRecord.value(), StandardCharsets.UTF_8));
    }

    private Optional<EncryptionMetadata> getEncryptionMetadata(ConsumerRecord<String, byte[]> kafkaRecord) {
        var aesKeyId = getFirstHeaderValue("aes_key_id", kafkaRecord).orElse(null);
        var aesIv = getFirstHeaderValue("aes_iv", kafkaRecord).orElse(null);

        var aesKeyIdStr = aesKeyId == null ? "" : new String(aesKeyId, StandardCharsets.UTF_8);

        if (aesKeyIdStr.equals(PLAINTEXT_KEY_ID) || aesKeyIdStr.isEmpty()) {
            return Optional.of(new EncryptionMetadata(PLAINTEXT_KEY_ID, new byte[]{}, new byte[]{}));
        }

        var aesKey = cluster.getAesKeys().stream().filter(key -> {
            var sha256 = Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString();
            return aesKeyIdStr.endsWith(sha256);
        }).findFirst().map(key -> Base64.getDecoder().decode(key)).orElse(null);
        if (aesKey == null) {
            App.getAppController().getStatusController().setEastStatus("No AES key with ID " + aesKeyIdStr + " provided");
            return Optional.empty();
        }

        if (aesIv == null || aesIv.length != 12) {
            App.getAppController().getStatusController().setEastStatus("IV is invalid: " + (aesIv == null ? "missing" : "expected 32 bytes, got " + aesIv.length));
            return Optional.empty();
        }
        return Optional.of(new EncryptionMetadata(aesKeyIdStr, aesKey, aesIv));
    }

    private Optional<byte[]> decrypt(byte[] encryptedBytes, EncryptionMetadata encryptionMetadata) {
        Stopwatch sw = Stopwatch.createStarted();
        if (encryptionMetadata.aesKeyId.matches(PLAINTEXT_KEY_ID)) {
            App.getAppController().getStatusController().setEastStatus("Plaintext");
            return Optional.of(encryptedBytes);
        }

        SecretKeySpec secretKey = new SecretKeySpec(encryptionMetadata.aesKey, "AES");
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM_MODE_AND_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(8 * encryptionMetadata.aesIv.length, encryptionMetadata.aesIv));

            byte[] plaintextBytes = cipher.doFinal(encryptedBytes);

            App.getAppController().getStatusController().setEastStatus(String.format("Decrypted in %s with key ID %s", sw, encryptionMetadata.aesKeyId));

            return Optional.of(plaintextBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            App.getAppController().getStatusController().setEastStatus("Cannot decrypt: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<byte[]> getFirstHeaderValue(String key, ConsumerRecord<?, ?> kafkaRecord) {
        var iterator = kafkaRecord.headers().headers(key).iterator();
        if (iterator.hasNext()) {
            return Optional.of(iterator.next().value());
        }
        return Optional.empty();
    }

    public void start() {
        new Thread(() -> {
            startAction.setEnabled(false);
            stopAction.setEnabled(true);
            consumerModel.start((records) -> {
                if (!records.isEmpty()) {
                    var selectedRow = pane.getTable().getSelectedRow();
                    var selectedRecord = -1;
                    if (selectedRow != -1) {
                        selectedRecord = consumerTableModel.getSelectedRecordIndex(selectedRow);
                    }
                    for (var record : records) {
                        consumerTableModel.addRecord(record);
                    }
                    consumerTableModel.fireTableDataChanged();
                    if (selectedRecord != -1) {
                        pane.getTable().getSelectionModel().setSelectionInterval(0, consumerTableModel.getRowIndex(selectedRecord));
                    }
                }
            });
        }).start();
    }

    public void stop() {
        new Thread(() -> {
            consumerModel.stop();
            startAction.setEnabled(true);
            stopAction.setEnabled(false);
        }).start();
    }

    public void clear() {
        consumerTableModel.clear();
        consumerTableModel.fireTableDataChanged();
    }

    public ConsumerDetailsPane getPane() {
        return pane;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Consumer getConsumer() {
        return consumer;
    }
}
