package kafkavisualizer.details.consumer;

import com.google.common.collect.Iterables;
import kafkavisualizer.models.Cluster;
import kafkavisualizer.models.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConsumerModel {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerModel.class);

    private final Cluster cluster;
    private final Consumer consumer;
    private final Properties properties;
    private boolean started;

    private KafkaConsumer<String, byte[]> kafkaConsumer;

    public ConsumerModel(Cluster cluster, Consumer consumer) {
        this.cluster = cluster;
        this.consumer = consumer;
        properties = new Properties();
        properties.put("bootstrap.servers", cluster.getServers());
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    }

    public void start(java.util.function.Consumer<ConsumerRecords<String, byte[]>> callback) {
        if (started) {
            return;
        }

        try {
            started = true;
            kafkaConsumer = new KafkaConsumer<>(properties);
            var partitions = kafkaConsumer.partitionsFor(consumer.getTopics().get(0))
                    .stream()
                    .map(p -> new TopicPartition(p.topic(), p.partition()))
                    .collect(Collectors.toList());
            kafkaConsumer.assign(partitions);
            if (consumer.getStartFrom() == Consumer.StartFrom.BEGINNING) {
                kafkaConsumer.seekToBeginning(partitions);
            }

            //noinspection InfiniteLoopStatement
            while (true) {
                var records = kafkaConsumer.poll(Duration.ofSeconds(120));
                LOG.info("Polled record: {}", Iterables.size(records));

                callback.accept(records);
            }
        } catch (WakeupException ex) {
            started = false;
        }
        finally {
            kafkaConsumer.close(Duration.ofMillis(500));
        }
    }

    public void stop() {
        if (!started) {
            return;
        }
        kafkaConsumer.wakeup();
    }

    public boolean isStarted() {
        return started;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Consumer getConsumer() {
        return consumer;
    }
}
