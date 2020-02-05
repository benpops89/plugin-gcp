package org.kestra.task.gcp.bigquery;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.kestra.core.models.tasks.RunnableTask;
import org.kestra.core.runners.RunContext;
import org.kestra.core.serializers.JacksonMapper;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Load extends AbstractLoad implements RunnableTask<AbstractLoad.Output> {
    private String from;

    private String projectId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        BigQuery connection = new Connection().of(runContext.render(this.projectId));
        Logger logger = runContext.logger(this.getClass());

        WriteChannelConfiguration.Builder builder = WriteChannelConfiguration
            .newBuilder(Connection.tableId(this.destinationTable));

        this.setOptions(builder);

        WriteChannelConfiguration configuration = builder.build();
        logger.debug("Starting load\n{}", JacksonMapper.log(configuration));

        URI from = new URI(runContext.render(this.from));
        InputStream data = runContext.uriToInputStream(from);

        TableDataWriteChannel writer = connection.writer(configuration);
        try (OutputStream stream = Channels.newOutputStream(writer)) {
            byte[] buffer = new byte[10_240];

            int limit;
            while ((limit = data.read(buffer)) >= 0) {
                writer.write(ByteBuffer.wrap(buffer, 0, limit));
            }
        }

        return this.execute(runContext, logger, configuration, writer.getJob());
    }
}