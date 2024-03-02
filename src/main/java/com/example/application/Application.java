package com.example.application;

import com.example.application.services.BookingTools;
import com.example.application.services.CustomerSupportAgent;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiModelName.GPT_4;


@SpringBootApplication
@Theme(value = "customer-support-agent")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    Tokenizer tokenizer() {
        return new OpenAiTokenizer(GPT_4);
    }


    // In the real world, ingesting documents would often happen separately, on a CI server or similar
    @Bean
    CommandLineRunner docsToEmbeddings(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            Tokenizer tokenizer,
            ResourceLoader resourceLoader
    ) throws IOException {
        return args -> {
            Resource resource =
                    resourceLoader.getResource("classpath:terms-of-service.txt");
            var termsOfUse = loadDocument(resource.getFile().toPath(), new TextDocumentParser());

            DocumentSplitter documentSplitter = DocumentSplitters.recursive(200, 0,
                    tokenizer);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(termsOfUse);
        };
    }

    @Bean
    StreamingChatLanguageModel chatLanguageModel(@Value("${azure.openai.key}") String apiKey,
          @Value("${azure.openai.endpoint}") String endpoint,
          @Value("${azure.deployment.name}") String deploymentName) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deploymentName)
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();
    }


    @Bean
    ContentRetriever retriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();
    }


    @Bean
    CustomerSupportAgent customerSupportAgent(
            StreamingChatLanguageModel chatLanguageModel,
            Tokenizer tokenizer,
            ContentRetriever retriever,
            BookingTools tools
    ) {

        return AiServices.builder(CustomerSupportAgent.class)
                .streamingChatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatId -> TokenWindowChatMemory.builder()
                        .id(chatId)
                        .maxTokens(1000, tokenizer)
                        .build())
                .contentRetriever(retriever)
                .tools(tools)
                .build();
    }
}
