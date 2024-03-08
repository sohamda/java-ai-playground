package com.example.application;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.example.application.services.BookingTools;
import com.example.application.services.FlightService;
import com.example.application.services.SearchTool;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.ai.embeddings.TextEmbeddingGeneration;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.connectors.ai.openai.util.ClientType;
import com.microsoft.semantickernel.connectors.ai.openai.util.OpenAIClientProvider;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.memory.MemoryStore;
import com.microsoft.semantickernel.memory.VolatileMemoryStore;
import com.microsoft.semantickernel.planner.actionplanner.ActionPlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.DefaultStepwisePlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlanner;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpringBootApplication
@Theme(value = "customer-support-agent")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    // In the real world, ingesting documents would often happen separately, on a CI server or similar
    @Bean
    CommandLineRunner docsToEmbeddings(Kernel embeddingKernel, ResourceLoader resourceLoader) {
        return args -> {
            Resource resource =
                    resourceLoader.getResource("classpath:terms-of-service.txt");

            embeddingKernel.getMemory().saveInformationAsync("termsOfService",
                    resource.getContentAsString(StandardCharsets.UTF_8),
                    "termsOfService",
                    "terms of service of Funnair", null);
        };
    }

    @Bean
    Kernel embeddingKernel(@Value("${client.azureopenai.key}") String apiKey,
                           @Value("${client.azureopenai.endpoint}") String endpoint,
                           @Value("${client.azure.embedding.model}") String modelName) throws ConfigurationException {
        OpenAIAsyncClient openAIAsyncClient = getOpenAIAsyncClient(apiKey, endpoint);

        TextEmbeddingGeneration textEmbeddingGenerationService =
                SKBuilders.textEmbeddingGeneration()
                        .withOpenAIClient(openAIAsyncClient)
                        .withModelId(modelName)
                        .build();
        MemoryStore memoryStore = new VolatileMemoryStore.Builder().build();

        return SKBuilders.kernel()
                .withDefaultAIService(textEmbeddingGenerationService)
                .withMemoryStorage(memoryStore)
                .build();
    }

    @Bean
    ActionPlanner actionPlanner(Kernel kernelWithSkills) throws ConfigurationException {

        //return new DefaultStepwisePlanner(kernelWithSkills, null, null);


        return new ActionPlanner(kernelWithSkills, null);
    }

    @Bean
    Kernel kernelWithSkills(@Value("${client.azureopenai.key}") String apiKey,
                            @Value("${client.azureopenai.endpoint}") String endpoint,
                            @Value("${client.azureopenai.deploymentname}") String modelName,
                            BookingTools bookingTools, SearchTool searchTool, Kernel embeddingKernel, FlightService flightService) throws ConfigurationException {
        Kernel kernel = SKBuilders.kernel()
                .withDefaultAIService(SKBuilders.chatCompletion()
                        .withModelId(modelName)
                        .withOpenAIClient(getOpenAIAsyncClient(apiKey, endpoint))
                        .build())
                .build();

        kernel.importSkill(bookingTools, "Plugin to get booking details or to update or delete bookings based on parameters such as {{$bookingNumber}}, {{$firstName}} and {{$lastName}}, {{$date}}, {{$from}} and {{$to}}");
        kernel.importSkill(searchTool, "find the Terms of Service for flight booking, change or update, cancellation for {{$input}}");
        return kernel;
    }

    private static OpenAIAsyncClient getOpenAIAsyncClient(String apiKey, String endpoint) throws ConfigurationException {
        Map<String, String> azureOpenAIConfig =
                Map.of("client.azureopenai.key", apiKey,
                        "client.azureopenai.endpoint", endpoint);
        OpenAIClientProvider clientProvider = new OpenAIClientProvider(azureOpenAIConfig, ClientType.AZURE_OPEN_AI);
        return clientProvider.getAsyncClient();
    }

    @Bean
    Kernel chatAssistantKernel(@Value("${client.azureopenai.key}") String apiKey,
                                         @Value("${client.azureopenai.endpoint}") String endpoint,
                                         @Value("${client.azureopenai.deploymentname}") String modelName) throws ConfigurationException {
        Kernel kernel = SKBuilders.kernel()
                .withAIService("chatAssistant",
                        SKBuilders.chatCompletion()
                        .withModelId(modelName)
                        .withOpenAIClient(getOpenAIAsyncClient(apiKey, endpoint))
                        .build(), true, ChatCompletion.class)
                .build();
        return kernel;
    }
}
