package org.vaadin.marcus.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionInvocation;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.ServiceNotFoundException;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class Evaluator {

    @SpyBean
    SKAssistant skAssistant;

    Kernel evaluator;
    KernelPlugin relevance;

    @Value("${sk.openai.key}") String apiKey;
    @Value("${sk.azure.openai.endpoint}") String endpoint;
    @Value("${sk.deployment.name}") String deploymentName;

    void initializeKernel() {
        OpenAIAsyncClient openAIAsyncClient;
        if(StringUtils.hasLength(endpoint)) {
            openAIAsyncClient = new OpenAIClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(apiKey))
                    .buildAsyncClient();
        } else {
            openAIAsyncClient = new OpenAIClientBuilder()
                    .credential(new KeyCredential(apiKey))
                    .buildAsyncClient();
        }

        OpenAIChatCompletion chatCompletionService = OpenAIChatCompletion.builder()
                .withOpenAIAsyncClient(openAIAsyncClient)
                .withModelId(deploymentName)
                .build();
        relevance = KernelPluginFactory
                .importPluginFromDirectory(Path.of("src/test/resources/Skills"),
                        "EvaluationSkills",null);

        evaluator = Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletionService)
                .withPlugin(relevance)
                .build();
    }

    @ParameterizedTest
    @CsvSource({"what is the cancellation policy," +
            "Cancelling Bookings\n" +
            "  - Cancel up to 48 hours before flight.\n" +
            "  - Cancellation fees: $75 for Economy, $50 for Premium Economy, $25 for Business Class.\n" +
            "  - Refunds processed within 7 business days."})
    void evaluateResponse(String query, String groundTruth) throws ServiceNotFoundException {
        initializeKernel();
        // do
        Flux<String> policy = skAssistant.chat("first", query);
        // when & then
        Mono.when(policy.collectList().doOnNext(response -> {
            relevanceCheck(response, query);
            accuracyCheck(response, groundTruth);
        })).block();
    }

    private void relevanceCheck(List<String> response, String question) {
        KernelFunctionArguments arguments = KernelFunctionArguments
                .builder()
                .withVariable("question", question)
                .withVariable("answer", response.get(0))
                .build();
        FunctionInvocation<String> result = evaluator.invokeAsync(relevance.<String>get("Relevance"))
                .withArguments(arguments);
        StepVerifier.create(result.log()).assertNext(r -> {
            System.out.println("Relevancy is :: " + r.getResult());
            assertThat(r.getResult()).isGreaterThanOrEqualTo("0.9");
        }).expectComplete().verify();

    }

    private void accuracyCheck(List<String> response, String groundTruth) {
        KernelFunctionArguments arguments = KernelFunctionArguments
                .builder()
                .withVariable("text1", groundTruth)
                .withVariable("text2", response.get(0))
                .build();
        FunctionInvocation<String> result = evaluator.invokeAsync(relevance.<String>get("Accuracy"))
                .withArguments(arguments);
        StepVerifier.create(result.log()).assertNext(r -> {
            System.out.println("Accuracy is :: " + r.getResult());
            assertThat(r.getResult()).isGreaterThanOrEqualTo("0.8");
        }).expectComplete().verify();

    }
}
