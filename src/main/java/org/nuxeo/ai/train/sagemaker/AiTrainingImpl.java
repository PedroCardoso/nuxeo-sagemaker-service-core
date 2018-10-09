package org.nuxeo.ai.train.sagemaker;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sagemaker.AmazonSageMakerAsyncClient;
import com.amazonaws.services.sagemaker.AmazonSageMakerAsyncClientBuilder;
import com.amazonaws.services.sagemaker.AmazonSageMakerClient;
import com.amazonaws.services.sagemaker.AmazonSageMakerClientBuilder;
import com.amazonaws.services.sagemaker.model.*;
import org.nuxeo.runtime.aws.NuxeoAWSCredentialsProvider;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class AiTrainingImpl extends DefaultComponent implements AiTraining {
    public static final String AWS_ROLE = "arn:aws:iam::783725821734:role/service-role/AmazonSageMaker-ExecutionRole-20180314T123968";
    public static final String SAGEMAKER_TF_IMAGE = "783725821734.dkr.ecr.us-east-1.amazonaws.com/sagemaker-nxai:1.10-0.0.1-cpu-py2";
    public static final String SAGEMAKER_INSTANCE = "ml.m5.2xlarge";
    public static final int SAGEMAKER_INSTANCE_DISK_SIZE_GB = 5;
    /**
     * Component activated notification.
     * Called when the component is activated. All component dependencies are resolved at that moment.
     * Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification.
     * Called before a component is unregistered.
     * Use this method to do cleanup if any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification.
     * Called after the application started.
     * You can do here any initialization that requires a working application
     * (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Add some logic here to handle contributions
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
    }

    private Map<String, String> generateParameters(String aiModelId, String[] aiCorpusIds){
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("layer1", "300");
        parameters.put("layer2", "50");
        parameters.put("layer3", "0");
        parameters.put("dropout", "0.2");
        parameters.put("learning_rate", "0.001");
        parameters.put("optimizer_momentum", "0");1
        parameters.put("optimizer_adam", "1");
        parameters.put("optimizer_adagrad", "0");

        // labels inputs
        parameters.put("labels", "[\"LOC\", \"HUM\", \"NUM\", \"ABBR\", \"ENTY\", \"DESC\"]");
        parameters.put("output_name", "\"answer\"");
        parameters.put("label_field", "\"label\"");
        parameters.put("language", "\"en\"");
        parameters.put("fields", "[[\"question\", \"txt\"], [\"label\", \"lab\"]]");

        parameters.put("epochs", "null");
        parameters.put("batch_size", "32");

        parameters.put("sagemaker_container_log_level", "20");
        parameters.put("sagemaker_enable_cloudwatch_metrics", "false");
        parameters.put("sagemaker_region", "\"us-east-1\"");

        parameters.put("sagemaker_program", "\"customAiModel.py\"");
        //parameters.put("sagemaker_program", "\"sagemakerCustomEstimatorHub.py\"");

        parameters.put("sagemaker_submit_directory", "\"s3://sagemaker-us-east-test/trec-6/train/train_code/train-trec-6-custom-2018-09-28-17-09-11-595/source/sourcedir.tar.gz\"");
        parameters.put("save_checkpoints_secs", "null");
        parameters.put("save_checkpoints_steps", "511");
        parameters.put("training_steps", "3407");
        parameters.put("throttle_secs", "1");

        parameters.put("checkpoint_path","\"s3://sagemaker-us-east-test/trec-6/train2/models_checkpoint/dnn\"");
        return parameters;
    }

    private List<Tag> generateTags(){
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withKey("billing-category").withValue("internal"));
        tags.add(new Tag().withKey("billing-subcategory").withValue("sagemaker"));
        tags.add(new Tag().withKey("contact").withValue("pcardoso@nuxeo.com"));
        return tags;
    }

    protected List<Channel> getDataInfo(String[] aiCorpusIds){
        S3DataSource s3Data = new S3DataSource()
                .withS3Uri("s3://sagemaker-us-east-test/trec-6/data")
                .withS3DataDistributionType(S3DataDistribution.FullyReplicated)
                .withS3DataType(S3DataType.S3Prefix);
        Channel trainingData = new Channel()
                .withChannelName("training")
                .withDataSource(new DataSource().withS3DataSource(s3Data));
        List<Channel> inputData = new ArrayList<>();
        inputData.add(trainingData);
        return inputData;
    }

    protected String getJobName(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        String dataToAdd = dtf.format(now);
        return "Nuxeo-test8_"+dataToAdd;
    }

    @Override
    public void train(String ModelId, String[] aiCorpusIds, String[] aiCorpusEvIds) {
        // create a client builder and the client
        AWSCredentialsProvider credentialsProvider = NuxeoAWSCredentialsProvider.getInstance();
        String region = NuxeoAWSRegionProvider.getInstance().getRegion();
        AmazonSageMakerClientBuilder builder =
                AmazonSageMakerClientBuilder.standard()
                        .withCredentials(credentialsProvider)
                        .withRegion(region);
        AmazonSageMakerClient sagemakerClient = (AmazonSageMakerClient) builder.build();

        // prepare request for training job
        AlgorithmSpecification algorithmSpecs = new AlgorithmSpecification()
                .withTrainingImage(SAGEMAKER_TF_IMAGE)
                .withTrainingInputMode(TrainingInputMode.File);
        ResourceConfig resources = new ResourceConfig()
                .withInstanceCount(1)
                .withInstanceType(SAGEMAKER_INSTANCE)
                .withVolumeSizeInGB(SAGEMAKER_INSTANCE_DISK_SIZE_GB);

        CreateTrainingJobRequest jobRequest = new CreateTrainingJobRequest()
                .withAlgorithmSpecification(algorithmSpecs)
                .withHyperParameters(generateParameters(ModelId, aiCorpusIds))
                .withInputDataConfig(getDataInfo(aiCorpusIds))
                .withOutputDataConfig(new OutputDataConfig().withS3OutputPath("s3://sagemaker-us-east-test/trec-6/train2"))
                .withResourceConfig(resources)
                .withRoleArn(AWS_ROLE)
                .withTags(generateTags())
                .withStoppingCondition(new StoppingCondition().withMaxRuntimeInSeconds(600))
                .withTrainingJobName(getJobName());

        CreateTrainingJobResult job = sagemakerClient.createTrainingJob(jobRequest);
        String res = job.toString();
        System.out.println(res);


    }
}
