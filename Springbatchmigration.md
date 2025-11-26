Skip to content
Navigation Menu
Platform
Solutions
Resources
Open Source
Enterprise
Pricing

Search or jump to...
Sign in
Sign up
spring-projects
/
spring-batch
Public
Code
Issues
287
Pull requests
29
Discussions
Actions
Wiki
Security
Insights
Spring Batch 6.0 Migration Guide
Mahmoud Ben Hassine edited this page 20 hours ago · 39 revisions
This document is meant to help you migrate your applications to Spring Batch 6.0. Spring Batch 6 does not change the minimum required Java version which remains Java 17+.

❗ Heads-up: While we tried to carefully list all changes here, we might have missed an API or two. If that is the case, then please open an issue by reporting the missing information and we will update this migration guide accordingly.

❗ Migration support: if you still need help migrating your application to Spring Batch 6, please open a "Migration Support" request in GitHub Discussions and we will help you there.

Major changes
Dependencies upgrade
Spring Batch 6 upgrades its Spring dependencies to the following versions:

Spring Framework 7
Spring Integration 7
Spring Data 4
Spring AMQP 4
Spring for Apache Kafka 4
Micrometer 1.16
Batch domain model changes
The Batch domain model was redesigned towards immutability. This had the following consequences in terms of API changes:

It is not possible to re-assign an ID to any class extending org.springframework.batch.core.Entity (ie JobInstance, JobExecution, etc)
Entity IDs have been changed from the wrapper Long type to the primitive long type. This is to avoid nullability issues in the JobRepository and JobOperator APIs
It is not possible to create orphan entities anymore (ie a job execution without a parent job instance, or a step execution without a parent job execution)
It is not possible to query for "shallow" entities anymore (as in v5). Querying the job repository for an entity will return the entity and its siblings. We do not recommend keeping references to many job instances in memory, and a careful retention policy is now required on the client side. This issue is a no problem when following the best practice of running a single job in its own JVM.
It is not possible to create batch artefacts (item readers, writers, etc) with default constructors + setters + afterPropertiesSet call. Required dependencies must now be specified at construction time.
JobParameter is now a record (immutable by design/construction) and encapsulates the parameter name (which was previously the key of the Map<String, JobParameter> that backs the JobParameters class)
Infrastructure beans configuration
Changes related to @EnableBatchProcessing
Before v6, the @EnableBatchProcessing annotation was tied to a JDBC-based infrastructure. This is not the case anymore. Two new annotations have been introduced to configure the underlying job repository: @EnableJdbcJobRepository and @EnableMongoJobRepository.

Starting from v6, @EnableBatchProcessing allows you to configure common attributes for the batch infrastructure, while store-specific attributes can be specified with the new dedicated annotations. Here is an example to migrate a JDBC configuration from v5 to v6:

// v5
@EnableBatchProcessing(dataSourceRef = "batchDataSource", taskExecutorRef = "batchTaskExecutor")
class MyJobConfiguration {

	@Bean
	public Job job(JobRepository jobRepository) {
		return new JobBuilder("job", jobRepository)
                    // job flow omitted
                    .build();
	}
}
// v6
@EnableBatchProcessing(taskExecutorRef = "batchTaskExecutor")
@EnableJdbcJobRepository(dataSourceRef = "batchDataSource")
class MyJobConfiguration {

	@Bean
	public Job job(JobRepository jobRepository) {
		return new JobBuilder("job", jobRepository)
                    // job flow omitted
                    .build();
	}
}
Changes related to the modular batch configurations through @EnableBatchProcessing(modular = true)
Modular batch configurations through @EnableBatchProcessing(modular = true) have been deprecated in favor of using Spring's context hierarchies and GroupAwareJobs. This section explains how to migrate the usage of that feature to v6.

Job configurations that might result in bean naming clashes should be separated in different classes (same as v5):

@Configuration
@Import(CommonBatchConfiguration.class)
public class FooJobConfiguration {

    @Bean
    public Job job(JobRepository jobRepository) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            System.out.println("Executing Foo Job");
            return RepeatStatus.FINISHED;
        };
        return new JobBuilder("foo", jobRepository)
                .start(new StepBuilder("step", jobRepository)
                        .tasklet(tasklet)
                        .build()).build();
    }
}

@Configuration
@Import(CommonBatchConfiguration.class)
public class BarJobConfiguration {

    @Bean
    public Job job(JobRepository jobRepository) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            System.out.println("Executing Bar Job");
            return RepeatStatus.FINISHED;
        };
        return new JobBuilder("bar", jobRepository)
                .start(new StepBuilder("step", jobRepository)
                        .tasklet(tasklet)
                        .build()).build();
    }
}
In this example, note how the job bean name is the same. Common batch configuration can be defined in a shared class, for example CommonBatchConfiguration:

@EnableBatchProcessing
@EnableJdbcJobRepository
public class CommonBatchConfiguration {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
                .addScript("/org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    @Bean
    public JdbcTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

}
Once that in place, you can define two Spring application contexts (one for each job configuration) having a base context with the common configuration as parent:

ApplicationContext baseContext = new AnnotationConfigApplicationContext(CommonBatchConfiguration.class);
JobOperator jobOperator = baseContext.getBean(JobOperator.class);

AnnotationConfigApplicationContext fooContext = new AnnotationConfigApplicationContext(FooJobConfiguration.class);
fooContext.setParent(baseContext);
Job fooJob = fooContext.getBean(Job.class);
jobOperator.start(fooJob, new JobParameters());

AnnotationConfigApplicationContext barContext = new AnnotationConfigApplicationContext(BarJobConfiguration.class);
barContext.setParent(baseContext);
Job barJob = barContext.getBean(Job.class);
jobOperator.start(barJob, new JobParameters());
Changes related to DefaultBatchConfiguration
Similar to the annotation-based approach, the programmatic model based on DefaultBatchConfiguration has been updated by introducing two new configuration classes to define store-specific attributes: JdbcDefaultBatchConfiguration and MongoDefaultBatchConfiguration.

Here is an example to migrate a JDBC configuration from v5 to v6:

// v5
class MyJobConfiguration extends DefaultBatchConfiguration  {

    @Bean
    public Job job(JobRepository jobRepository) {
        return new JobBuilder("job", jobRepository)
                // job flow omitted
                .build();
    }

    @Override
    protected DataSource getDataSource() {
        return new MyBatchDataSource();
    }
}
// v6
class MyJobConfiguration extends JdbcDefaultBatchConfiguration  {

    @Bean
    public Job job(JobRepository jobRepository) {
        return new JobBuilder("job", jobRepository)
                // job flow omitted
                .build();
    }

    @Override
    protected DataSource getDataSource() {
        return new MyBatchDataSource();
    }
}
Changes related to the default batch configuration
The DefaultBatchConfiguration now configures a "resourceless" batch infrastructure (ie a ResourcelessJobRepository and a ResourcelessTransactionManager). This is to remove the need for an additional dependency to an in-memory database for those who do not need batch meta-data.
The configuration of a JobExplorer bean has been removed. This is due to the fact that JobRepository now extends JobExplorer and there is no need for a JobExplorer bean anymore, which is now deprecated (See "Deprecated APIs" section).
The configuration of a JobLauncher has been replaced with a JobOperator. This is due to the fact that JobOperator now extends JobLauncher and there is no need for a JobLauncher bean anymore, which is now deprecated (See "Deprecated APIs" section).
The configuration of a JobRegistrySmartInitializingSingleton bean has been removed. The MapJobRegistry is now optional as well , and is smart enough to populate itself with jobs from the application context on startup. JobRegistrySmartInitializingSingleton is now deprecated (See "Deprecated APIs" section).
The configuration of a transaction manager in JobOperatorFactoryBean is now optional
The configuration of a transaction manager in tasklet and chunk-oriented steps is now optional
New chunk-oriented model implementation
The new implementation can be used by changing the following method call:

// legacy implementation (v5)
Step step = new StepBuilder("myStep", jobRepository)
.chunk(5, transactionManager)
// ...
.build();

// new implementation (v6)
Step step = new StepBuilder("myStep", jobRepository)
.chunk(5)
.transactionManager(transactionManager)
// ...
.build();
The legacy implementation will be available in a deprecated form for the entire v6 generation to give our users time to upgrade, but it will not be evolved or maintained in any way. Please consider moving to the new implementation in a timely manner during the lifetime of v6.

Changes related to job parameters
API changes
Before v6, the name of a job parameter lived outside the JobParameter class. It was used as a key of the internal map of JobParameters while it should be part of the JobParameter concept. This had a couple issues:

Part of the state of an entity/value object is "detached" and lives outside the entity/value object
As a Spring Batch user, when you have a reference to a JobParameter, you cannot get its name without having to look into the parent JobParameters
The main change in v6 is that the name of a job parameter is now part of the JobParameter class which is now a record and the JobParameters class now holds a Set<JobParameter> instead of a Map from "name" -> "nameless job parameter".

More details about this change can be found in https://github.com/spring-projects/spring-batch/issues/5010.

JobParametersIncrementer changes
The JobParametersIncrementer concept is a useful abstraction to use when there is a natural sequence of job instances (hourly, daily, etc). The goal of this abstraction is to bootstrap the initial set of job parameters and let the framework calculate the parameters of the next instance in the sequence. This was clear since the initial design of the JobOperator#startNextInstance(String jobName) method (notice the lack of job parameters in the signature). The concept is similar to database sequences that are initialized once by the user and incremented automatically by the database system (ie not altered by the user anymore). Therefore, it does not make sense to provide job parameters when using an incrementer. If someone starts to modify the sequence's logic and altering job parameters in between instances, (s)he should not use an incrementer in the first place.

Starting from v6, When a job parameters incrementer is attached to a job definition, the parameters of the next instance will be calculated by the framework, and any additional parameters supplied by the user will be ignored with a warning.

Please refer to https://github.com/spring-projects/spring-batch/issues/4910 for more details about this change.

Database schema changes
The sequence BATCH_JOB_SEQ was renamed to BATCH_JOB_INSTANCE_SEQ
Migration scripts to v6 can be found here: https://github.com/spring-projects/spring-batch/tree/main/spring-batch-core/src/main/resources/org/springframework/batch/core/migration/6.0

Observability changes
The usage of Micrometer's global static meter registry was removed. It is now required to configure an ObservationRegistry as a bean in the application context to collect batch metrics. Here is a typical configuration:

@Configuration
@EnableBatchProcessing
static class MyJobConfiguration {

    @Bean
    public Job job(JobRepository jobRepository) {
        return new JobBuilder(jobRepository)
                   // job flow omitted
                   .build();
    }

    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry));
        return observationRegistry;
    }

    // Defining the MeterRegistry as a bean is up to the user
    @Bean
    public SimpleMeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

}
A complete example can be found in ChunkOrientedStepObservabilityIntegrationTests

Deprecated features and APIs
Deprecated features
The batch: and batch-integration: XML namespaces are deprecated in favor of the Java configuration style. You can continue using the XML configuration style, but the XML namespaces will not evolve anymore and are scheduled for removal in v7
Modular configurations through the @EnableBatchProcessing(modular = true) is deprecated in favor of Spring's context hierarchies and GroupAwareJobs, and is scheduled for removal in v6.2, see this section
JUnit 4 support is now deprecated when using @SpringBatchTest in favor of JUnit 5+ (we recommend upgrading to JUnit 6).
Deprecated APIs
The following APIs have been deprecated in version 6.0:

Deprecated classes and interfaces
org.springframework.batch.core.step.item.SkipOverflowException
org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException
org.springframework.batch.core.step.StepLocatorStepFactoryBean
org.springframework.batch.support.transaction.TransactionAwareProxyFactory
FaultTolerantStepBuilder
SimpleStepBuilder
BatchListenerFactoryHelper
FaultTolerantStepFactoryBean
SimpleStepFactoryBean
BatchRetryTemplate
ChunkMonitor
ChunkOrientedTasklet
ChunkProcessor
ChunkProvider
DefaultItemFailureHandler
FaultTolerantChunkProcessor
FaultTolerantChunkProvider
KeyGenerator
SimpleChunkProcessor
SimpleChunkProvider
SimpleRetryExceptionHandler
LimitCheckingItemSkipPolicy
org.springframework.batch.core.repository.support.JobRepositoryFactoryBean: renamed to JdbcJobRepositoryFactoryBean
org.springframework.batch.core.repository.explore.support.JobExplorerFactoryBean: renamed to JdbcJobExplorerFactoryBean
org.springframework.batch.core.configuration.annotation.AutomaticJobRegistrarBeanPostProcessor
org.springframework.batch.core.configuration.support.AbstractApplicationContextFactory
org.springframework.batch.core.configuration.support.ApplicationContextFactory
org.springframework.batch.core.configuration.support.ApplicationContextJobFactory
org.springframework.batch.core.configuration.support.AutomaticJobRegistrar
org.springframework.batch.core.configuration.support.ClasspathXmlApplicationContextsFactoryBean
org.springframework.batch.core.configuration.support.GenericApplicationContextFactory
org.springframework.batch.core.configuration.support.JobFactoryRegistrationListener
org.springframework.batch.core.configuration.support.ReferenceJobFactory
org.springframework.batch.core.configuration.JobFactory
org.springframework.batch.core.configuration.JobLocator
org.springframework.batch.core.configuration.ListableJobLocator
org.springframework.batch.core.configuration.support.JobLoader
org.springframework.batch.core.configuration.support.DefaultJobLoader
org.springframework.batch.core.launch.support.CommandLineJobRunner
org.springframework.batch.core.launch.support.JvmSystemExiter
org.springframework.batch.core.launch.support.RuntimeExceptionTranslator
org.springframework.batch.core.launch.support.SystemExiter
org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
org.springframework.batch.core.launch.JobLauncher
org.springframework.batch.core.repository.explore.support.AbstractJobExplorerFactoryBean
org.springframework.batch.core.repository.explore.support.JdbcJobExplorerFactoryBean
org.springframework.batch.core.repository.explore.support.JobExplorerFactoryBean
org.springframework.batch.core.repository.explore.support.MongoJobExplorerFactoryBean
org.springframework.batch.core.repository.explore.support.SimpleJobExplorer
org.springframework.batch.core.repository.explore.JobExplorer
org.springframework.batch.core.resource.StepExecutionSimpleCompletionPolicy
org.springframework.batch.support.PropertiesConverter
org.springframework.batch.test.StepRunner
org.springframework.batch.item.SkipWrapper
Deprecated methods
org.springframework.batch.core.repository.JobRepository#getStepExecution(long, long)
StepBuilder#chunk(int chunkSize, PlatformTransactionManager transactionManager)
StepBuilder#chunk(CompletionPolicy completionPolicy, PlatformTransactionManager transactionManager)
StoppableTasklet#stop()
JobExplorer#isJobInstanceExists(String jobName, JobParameters jobParameters)
JobExplorer#findJobExecutions(JobInstance jobInstance)
JobExplorer#findJobInstancesByJobName(String jobName, int start, int count)
JobExplorer#findJobInstancesByName(String jobName, int start, int count)
SimpleJobExplorer#findJobInstancesByJobName(String jobName, int start, int count)
JobInstanceDao#findJobInstancesByName(String jobName, final int start, final int count)
JdbcJobInstanceDao#findJobInstancesByName(String jobName, final int start, final int count)
MongoJobInstanceDao#findJobInstancesByName(String jobName, final int start, final int count)
JobOperator#getExecutions(long instanceId)
JobOperator#getJobInstances(String jobName, int start, int count)
JobOperator#getJobInstance(String jobName, JobParameters jobParameters)
JobOperator#getRunningExecutions(String jobName)
JobOperator#getParameters(long executionId)
JobOperator#getSummary(long executionId)
JobOperator#getStepExecutionSummaries(long executionId)
JobOperator#start(String jobName, Properties parameters)
JobOperator#getJobNames
JobOperator#restart(long executionId)
JobOperator#startNextInstance(String jobName)
JobOperator#stop(long executionId)
JobOperator#abandon(long jobExecutionId)
SimpleJobOperator#start(String jobName, Properties parameters)
SimpleJobOperator#getJobNames
SimpleJobOperator#restart(long executionId)
SimpleJobOperator#startNextInstance(String jobName)
SimpleJobOperator#stop(long executionId)
SimpleJobOperator#abandon(long jobExecutionId)
SimpleJobOperator#getExecutions(long instanceId)
SimpleJobOperator#getJobInstances(String jobName, int start, int count)
SimpleJobOperator#getJobInstance(String jobName, JobParameters jobParameters)
SimpleJobOperator#getRunningExecutions(String jobName)
SimpleJobOperator#getParameters(long executionId)
SimpleJobOperator#getSummary(long executionId)
SimpleJobOperator#getStepExecutionSummaries(long executionId)
SimpleJobOperator#setJobParametersConverter(JobParametersConverter jobParametersConverter)
DefaultBatchConfiguration#getJobParametersConverter()
EnableBatchProcessing#modular
Chunk#Chunk(List<? extends W> items, List<SkipWrapper<W>> skips)
Chunk#getSkips()
Chunk#getErrors()
Chunk#skip(Exception e)
Chunk#getSkipsSize()
Chunk#isEnd()
Chunk#setEnd()
Chunk#isBusy()
Chunk#setBusy(boolean)
Chunk#clearSkips()
Chunk#getUserData()
Chunk#setUserData(Object)
Chunk#chunk(CompletionPolicy, PlatformTransactionManager)
ChunkListener#beforeChunk(ChunkContext)
ChunkListener#afterChunk(ChunkContext)
ChunkListener#afterChunkError(ChunkContext)
CompositeChunkListener#beforeChunk(ChunkContext)
CompositeChunkListener#afterChunk(ChunkContext)
CompositeChunkListener#afterChunkError(ChunkContext)
Please refer to the Javadoc of each API for more details about the suggested replacement.

Moved APIs
All APIs de     makfined in the spring-batch-infrastructure module were moved from org.springframework.batch.* to org.springframework.batch.infrastructure.*
All APIs under org.springframework.batch.core.explore package have been moved under org.springframework.batch.core.repository.explore
JdbcExecutionContextDao, JdbcJobExecutionDao, JdbcJobInstanceDao and JdbcStepExecutionDao have been moved from org.springframework.batch.core.repository.dao to org.springframework.batch.core.repository.dao.jdbc
MongoExecutionContextDao, MongoJobExecutionDao, MongoJobInstanceDao, MongoStepExecutionDao and MongoSequenceIncrementer have been moved from org.springframework.batch.core.repository.dao to org.springframework.batch.core.repository.dao.mongo
Partitioner, PartitionNameProvider, PartitionStep and StepExecutionAggregator have been moved from org.springframework.batch.core.partition.support to org.springframework.batch.core.partition
ChunkListener, ItemProcessListener, ItemReadListener, ItemWriteListener, JobExecutionListener, SkipListener, StepExecutionListener and StepListener have been moved from org.springframework.batch.core to org.springframework.batch.core.listener
Job, JobExecution, JobExecutionException, JobInstance, JobInterruptedException, JobKeyGenerator, DefaultJobKeyGenerator, StartLimitExceededException and UnexpectedJobExecutionException have been moved from org.springframework.batch.core to org.springframework.batch.core.job
CompositeJobParametersValidator, DefaultJobParametersValidator, JobParameter, JobParameters, JobParametersBuilder, JobParametersIncrementer, JobParametersInvalidException and JobParametersValidator have been moved from org.springframework.batch.core to org.springframework.batch.core.job.parameters
Step, StepContribution and StepExecution have been moved from org.springframework.batch.core to org.springframework.batch.core.step
RunIdIncrementer was moved from org.springframework.batch.core.job.launch.support to org.springframework.batch.core.job.parameters
DataFieldMaxValueJobParametersIncrementer was moved from org.springframework.batch.core.job.launch.support to org.springframework.batch.core.job.parameters
Removed APIs
The following APIs were deprecated in previous versions and have been removed in this release:

method org.springframework.batch.core.configuration.support.DefaultBatchConfiguration#getLobHandler
Attribute lobHandlerRef in @EnableBatchProcessing
Attribute lob-handler in XML element step
method org.springframework.batch.core.repository.dao.JdbcExecutionContextDao#setLobHandler
method org.springframework.batch.core.repository.dao.JdbcJobInstanceDao#setJobIncrementer
method org.springframework.batch.core.repository.support.JobRepositoryFactoryBean#setLobHandler
method org.springframework.batch.core.configuration.support.DefaultBatchConfiguration#jobLauncher
method org.springframework.batch.core.configuration.support.DefaultBatchConfiguration#jobOperator
method org.springframework.batch.core.configuration.support.DefaultBatchConfiguration#jobRegistryBeanPostProcessor
class org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor
method org.springframework.batch.core.explore.support.JobExplorerFactoryBean#setLobHandler
constructor org.springframework.batch.core.job.builder.JobBuilder#JobBuilder(String name)
class org.springframework.batch.core.listener.ChunkListenerSupport
class org.springframework.batch.core.listener.JobExecutionListenerSupport
class org.springframework.batch.core.listener.SkipListenerSupport
class org.springframework.batch.core.listener.StepExecutionListenerSupport
method org.springframework.batch.core.step.builder.AbstractTaskletStepBuilder#throttleLimit
constructor org.springframework.batch.core.step.builder.StepBuilder#StepBuilder(String name)
method org.springframework.batch.core.step.builder.TaskletStepBuilder#tasklet(Tasklet tasklet)
method org.springframework.batch.core.step.factory.SimpleStepFactoryBean#setThrottleLimit
classes org.springframework.batch.item.data.MongoItemReader and org.springframework.batch.item.data.builder.MongoItemReaderBuilder
method org.springframework.batch.item.data.MongoItemWriter#setDelete
method org.springframework.batch.item.data.builder.MongoItemWriterBuilder#delete(boolean delete)
class org.springframework.batch.item.data.Neo4jItemReader and org.springframework.batch.item.data.Neo4jItemWriter
method org.springframework.batch.item.database.support.SqlPagingQueryUtils#generateLimitGroupedSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery, String limitClause)
class org.springframework.batch.item.database.support.SqlWindowingPagingQueryProvider
class org.springframework.batch.repeat.listener.RepeatListenerSupport
method org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate#setThrottleLimit
class org.springframework.batch.support.SystemPropertyInitializer
constructor org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilder#RemoteChunkingManagerStepBuilder(String stepName)
method org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilder#RemoteChunkingManagerStepBuilder repository(JobRepository jobRepository)
constructor org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilder#RemotePartitioningManagerStepBuilder(String stepName)
constructor org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder#RemotePartitioningWorkerStepBuilder(String name)
method org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder#RemotePartitioningWorkerStepBuilder repository(JobRepository jobRepository)
method org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder#tasklet(Tasklet tasklet)
method org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder#chunk(CompletionPolicy completionPolicy)
Renamed APIs
The class org.springframework.batch.integration.chunk.ChunkHandler was renamed to org.springframework.batch.integration.chunk.ChunkRequestHandler
The setter org.springframework.batch.core.step.job.JobStep#setJobLauncher(JobLauncher jobLauncher) was renamed to setJobOperator(JobOperator jobOperator) and now requires a JobOperator instead of a JobLauncher
The setter org.springframework.batch.core.step.builder.JobStepBuilder#launcher(JobLauncher jobLauncher) was renamed to operator(JobOperator jobOperator) and now requires a JobOperator instead of a JobLauncher
The attribute job-launcher of the XML element step.job in spring-batch.xsd was renamed to job-operator and now requires a JobOperator instead of a JobLauncher
The constructor org.springframework.batch.integration.launch.JobLaunchingGateway(JobLauncher jobLauncher) now accepts a JobOperator instead of a JobLauncher
org.springframework.batch.integration.launch.JobLaunchingMessageHandler(JobLauncher jobLauncher) now accepts a JobOperator instead of a JobLauncher
The attribute job-launcher of the XML element job-launching-gateway in spring-batch-integration.xsd was renamed to job-operator and now requires a JobOperator instead of a JobLauncher
The setter org.springframework.batch.core.step.tasklet.SystemCommandTasklet#setJobExplorer(JobExplorer jobExplorer) was renamed to setJobRepository(JobRepository jobRepository) and now requires a JobRepository instead of a JobExplorer
The constructor org.springframework.batch.core.partition.support.RemoteStepExecutionAggregator(JobExplorer jobExplorer) now accepts a JobRepository instead of a JobExplorer
The setter org.springframework.batch.core.partition.support.RemoteStepExecutionAggregator#setJobExplorer(JobExplorer jobExplorer) was renamed to setJobRepository(JobRepository jobRepository) and now requires a JobRepository instead of a JobExplorer
Documentation changes
The Javadocs are now generated and collected as part of the Antora-based documentation process and are located under the same versioned directory of the reference documentation:

--https://docs.spring.io/spring-batch/docs/${version}/api/index.html
++https://docs.spring.io/spring-batch/reference/${version}/api/index.html
Starting from v6, you can find references docs and Java docs in the following locations:

Reference docs: https://docs.spring.io/spring-batch/reference/6.0/index.html
Javadocs: https://docs.spring.io/spring-batch/reference/6.0/api/index.html
Those can be accessed directly from the home page of the project: https://spring.io/projects/spring-batch#learn

Pages 7
Find a page…
Home
Release process for v4
Release process for v5
Release process for v6
Spring Batch 5.0 Migration Guide
Spring Batch 6.0 Migration Guide
Major changes
Dependencies upgrade
Batch domain model changes
Infrastructure beans configuration
Changes related to @EnableBatchProcessing
Changes related to the modular batch configurations through @EnableBatchProcessing(modular = true)
Changes related to DefaultBatchConfiguration
Changes related to the default batch configuration
New chunk-oriented model implementation
Changes related to job parameters
API changes
JobParametersIncrementer changes
Database schema changes
Observability changes
Deprecated features and APIs
Deprecated features
Deprecated APIs
Deprecated classes and interfaces
Deprecated methods
Moved APIs
Removed APIs
Renamed APIs
Documentation changes
Support Policy
Clone this wiki locally
https://github.com/spring-projects/spring-batch.wiki.git
Footer
© 2025 GitHub, Inc.
Footer navigation
Terms
Privacy
Security
Status
Community
Docs
Contact
Manage cookies
Do not share my personal information
