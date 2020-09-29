package org.kestra.task.gcp.bigquery;

import com.devskiller.friendly_id.FriendlyId;
import com.google.cloud.bigquery.*;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kestra.core.models.flows.Flow;
import org.kestra.core.runners.RunContext;
import org.kestra.core.runners.RunContextFactory;

import java.util.Collections;
import java.util.UUID;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
@Slf4j
class TableMetadataTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("${kestra.tasks.bigquery.project}")
    private String project;

    @Value("${kestra.tasks.bigquery.dataset}")
    private String dataset;

    private BigQuery connection;

    @BeforeEach
    private void init() {
        this.connection = new BigQueryService().of(project, "EU");
    }

    private Job query(String query) throws InterruptedException {
        System.out.println(query);
        return this.connection
            .create(JobInfo
                .newBuilder(QueryJobConfiguration.newBuilder(query).build())
                .setJobId(JobId.of(UUID.randomUUID().toString()))
                .build()
            )
            .waitFor();
    }

    @Test
    void table() throws Exception {
        String friendlyId = FriendlyId.createFriendlyId();

        query("CREATE TABLE `" + this.dataset + "." + friendlyId + "`" +
            "(product STRING, quantity INT64, date TIMESTAMP)" +
            " PARTITION BY DATE(date)" +
            " CLUSTER BY quantity" +
            " OPTIONS(" +
            "  expiration_timestamp=TIMESTAMP_ADD(" +
            "  CURRENT_TIMESTAMP(), INTERVAL 48 HOUR)," +
            "  friendly_name=\"new_view\"," +
            "  description=\"a view that expires in 2 days\"," +
            "  labels=[(\"org_unit\", \"development\")]" +
            ");"
        );

        TableMetadata task = TableMetadata.builder()
            .projectId(this.project)
            .dataset(this.dataset)
            .table(friendlyId)
            .build();

        TableMetadata.Output run = task.run(runContextFactory.of(ImmutableMap.of()));

        assertThat(run.getTable(), is(friendlyId));
        assertThat(run.getFriendlyName(), is("new_view"));
        assertThat(run.getDefinition().getSchema().getFields().size(), is(3));

        assertThat(run.getDefinition().getSchema().getFields().get(1).getType(), is(LegacySQLTypeName.INTEGER));
        assertThat(run.getDefinition().getStandardTableDefinition().getClustering().getFields().get(0), is("quantity"));
        assertThat(run.getDefinition().getStandardTableDefinition().getTimePartitioning().getField(), is("date"));
    }

    @Test
    void view() throws Exception {
        String friendlyId = FriendlyId.createFriendlyId();

        query("CREATE VIEW `" + this.dataset + "." + friendlyId + "`\n" +
            "OPTIONS(" +
            "  expiration_timestamp=TIMESTAMP_ADD(" +
            "  CURRENT_TIMESTAMP(), INTERVAL 48 HOUR)," +
            "  friendly_name=\"new_view\"," +
            "  description=\"a view that expires in 2 days\"," +
            "  labels=[(\"org_unit\", \"development\")]" +
            ")\n" +
            "AS SELECT 'name' as name, 'state' as state, 1.23 as float, 1 as int"
        );

        TableMetadata task = TableMetadata.builder()
            .projectId(this.project)
            .dataset(this.dataset)
            .table(friendlyId)
            .build();

        TableMetadata.Output run = task.run(runContextFactory.of(ImmutableMap.of()));

        assertThat(run.getTable(), is(friendlyId));
        assertThat(run.getFriendlyName(), is("new_view"));
        assertThat(run.getDefinition().getSchema().getFields().size(), is(4));

        assertThat(run.getDefinition().getSchema().getFields().get(2).getType(), is(LegacySQLTypeName.FLOAT));
    }


    @Test
    void dontExistsError() throws Exception {
        String friendlyId = FriendlyId.createFriendlyId();

        TableMetadata task = TableMetadata.builder()
            .projectId(this.project)
            .dataset(this.dataset)
            .table(friendlyId)
            .build();

        // flow is not created
        assertThrows(IllegalArgumentException.class, () -> {
            TableMetadata.Output run = task.run(runContextFactory.of(ImmutableMap.of()));
        });
    }

    @Test
    void dontExistsNoError() throws Exception {
        String friendlyId = FriendlyId.createFriendlyId();

        TableMetadata task = TableMetadata.builder()
            .projectId(this.project)
            .dataset(this.dataset)
            .table(friendlyId)
            .ifExists(TableMetadata.IfExists.SKIP)
            .build();

        TableMetadata.Output run = task.run(runContextFactory.of(ImmutableMap.of()));

        assertThat(run.getTable(), is(nullValue()));
    }
}