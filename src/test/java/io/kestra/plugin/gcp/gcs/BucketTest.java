package io.kestra.plugin.gcp.gcs;

public class BucketTest extends AbstractBucketTest {
    @Override
    public boolean inferProjectId() {
        return false;
    }
}
