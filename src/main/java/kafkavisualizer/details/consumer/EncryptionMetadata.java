package kafkavisualizer.details.consumer;

public class EncryptionMetadata {

    public final String aesKeyId;
    public final byte[] aesKey;
    public final byte[] aesIv;

    public EncryptionMetadata(String aesKeyId, byte[] aesKey, byte[] aesIv) {
        this.aesKeyId = aesKeyId;
        this.aesKey = aesKey;
        this.aesIv = aesIv;
    }
}
