package org.nuxeo.ai.train.sagemaker;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ai.train.sagemaker.nuxeo-sagemaker-service-core")
public class TestAiTraining {

    @Inject
    protected AiTraining aitraining;

    @Test
    public void testService() {
        assertNotNull(aitraining);

        aitraining.train(new String(), new String[1], new String[1]);
    }
}
