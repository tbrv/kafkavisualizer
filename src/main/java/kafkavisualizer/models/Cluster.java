package kafkavisualizer.models;

import java.util.ArrayList;
import java.util.List;

public final class Cluster {

    private String name;
    private String servers;
    private List<String> aesKeys;
    private List<Producer> producers;
    private List<Consumer> consumers;

    public Cluster(String name, String servers, List<String> aesKeys) {
        this.name = name;
        this.servers = servers;
        this.aesKeys = aesKeys;
        producers = new ArrayList<>();
        consumers = new ArrayList<>();
    }

    public Cluster() {
        this(null, null, new ArrayList<>());
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public List<Producer> getProducers() {
        return producers;
    }

    public void setProducers(List<Producer> producers) {
        this.producers = producers;
    }

    public List<Consumer> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<Consumer> consumers) {
        this.consumers = consumers;
    }

    public List<String> getAesKeys() {
        return aesKeys;
    }

    public void setAesKeys(List<String> aesKeys) {
        this.aesKeys = aesKeys;
    }
}
