package org.kestra.task.gcp.gcs;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.kestra.core.models.annotations.Example;
import org.kestra.core.models.annotations.Plugin;
import org.kestra.core.models.tasks.RunnableTask;
import org.kestra.core.runners.RunContext;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Update some bucket labels",
            code = {
                "name: \"my-bucket\"",
                "labels:",
                "  my-label: my-value"
            }
        )
    }
)
@Schema(
    title = "Update a bucket."
)
public class UpdateBucket extends AbstractBucket implements RunnableTask<AbstractBucket.Output> {
    @Override
    public AbstractBucket.Output run(RunContext runContext) throws Exception {
        Storage connection = new Connection().of(runContext.render(this.projectId));
        Logger logger = runContext.logger();
        BucketInfo bucketInfo = this.bucketInfo(runContext);

        logger.debug("Updating bucket '{}'", bucketInfo);
        Bucket bucket = connection.update(bucketInfo);

        return Output.of(bucket);
    }
}
