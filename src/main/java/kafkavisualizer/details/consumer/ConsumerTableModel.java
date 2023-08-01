package kafkavisualizer.details.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.swing.table.AbstractTableModel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ConsumerTableModel extends AbstractTableModel {

    private static final String[] COL_NAMES = {"Timestamp", "Topic", "Partition", "Offset", "Key", "Value"};
    private final List<ConsumerRecord<String, byte[]>> records = new ArrayList<>();
    private String searchText = null;
    private List<ConsumerRecord<String, byte[]>> filteredRecords = new ArrayList<>();
    private final Predicate<ConsumerRecord<String, byte[]>> filterPredicate = r -> searchText == null || searchText.trim().length() == 0 ||
            (r.value() != null && new String(r.value(), StandardCharsets.UTF_8).contains(searchText)) || (r.key() != null && r.key().contains(searchText));

    @Override
    public String getColumnName(int column) {
        return COL_NAMES[column];
    }

    @Override
    public int getRowCount() {
        return filteredRecords.size();
    }

    @Override
    public int getColumnCount() {
        return COL_NAMES.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var index = filteredRecords.size() - rowIndex - 1;
        var record = filteredRecords.get(index);

        switch (columnIndex) {
            case 0:
                return Instant.ofEpochMilli(record.timestamp());
            case 1:
                return record.topic();
            case 2:
                return record.partition();
            case 3:
                return record.offset();
            case 4:
                return record.key();
            case 5:
                return new String(record.value(), 0, Math.min(500, record.value().length));
            default:
                return "";
        }
    }

    public void addRecord(ConsumerRecord<String, byte[]> record) {
        records.add(record);
        if (filterPredicate.test(record)) {
            filteredRecords.add(record);
        }
    }

    public void clear() {
        records.clear();
        filteredRecords.clear();
    }

    public ConsumerRecord<String, byte[]> getSelectedRecord(int selectedRow) {
        return filteredRecords.get(filteredRecords.size() - selectedRow - 1);
    }

    public int getSelectedRecordIndex(int selectedRow) {
        return filteredRecords.size() - selectedRow - 1;
    }

    public int getRowIndex(int recordIndex) {
        return filteredRecords.size() - recordIndex - 1;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
        filteredRecords.clear();
        for (var record : records) {
            if (filterPredicate.test(record)) {
                filteredRecords.add(record);
            }
        }
    }
}
