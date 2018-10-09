package org.nuxeo.ai.train.sagemaker;

public interface AiTraining {
    public void train(String ModelId, String[] aiCorpusIds, String[] aiCorpusEvIds);

}
