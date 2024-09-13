package org.vaadin.marcus.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.textcompletion.OpenAITextGenerationService;
import com.microsoft.semantickernel.services.textcompletion.TextGenerationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest
public class ConversationQualityTest {

    private Kernel testKernel;
    @Value("${sk.openai.key}") String apiKey;
    @Value("${sk.azure.openai.endpoint}") String endpoint;
    @Value("${sk.deployment.name}") String deploymentName;

    @Value("classpath:validators/test-convo-1.json")
    Resource resourceFile;
    private List<Conversation> conversations;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    void initTestConfigs() throws IOException {
        conversations = objectMapper.readValue(resourceFile.getContentAsString(StandardCharsets.UTF_8),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Conversation.class));
        testKernel = initializeKernel();
    }

    private Kernel initializeKernel() {
        OpenAIAsyncClient client;

        if(StringUtils.hasLength(endpoint)) {
            client = new OpenAIClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(apiKey))
                    .buildAsyncClient();
        } else {
            client = new OpenAIClientBuilder()
                    .credential(new KeyCredential(apiKey))
                    .buildAsyncClient();
        }

        TextGenerationService textGenerationService = OpenAITextGenerationService.builder()
                .withOpenAIAsyncClient(client)
                .withModelId(deploymentName)
                .build();

        return Kernel.builder()
                .withAIService(TextGenerationService.class, textGenerationService)
                .build();
    }
}
