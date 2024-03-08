package com.example.application.services;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.memory.MemoryQueryResult;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
@Component
public class SearchTool {

    Logger log = LoggerFactory.getLogger(SearchTool.class);

    private final Kernel embeddingKernel;

    public SearchTool(Kernel embeddingKernel) {
        this.embeddingKernel = embeddingKernel;
    }

    @DefineSKFunction(name = "findFlightBookingPolicyDetails", description = "find the Terms of Service for flight booking, change or update, cancellation")
    public String findFlightBookingPolicyDetails(
            @SKFunctionParameters(name = "input", description = "user query to find a specific flight policy") String input) {

        log.debug("Invoking find flight policy details for question : {}", input);
        Mono<List<MemoryQueryResult>> relevantMemory = this.embeddingKernel.getMemory()
                .searchAsync("termsOfService", input, 2, 0.7f, false);
        List<MemoryQueryResult> relevantMems = relevantMemory.block();
        StringBuilder memory = new StringBuilder();
        relevantMems.forEach(relevantMem -> memory.append("info: ").append(relevantMem.getMetadata().getText()));
        String policiesFound = memory.toString();
        log.debug("Search results from embedding memory : {}", policiesFound);

        return policiesFound;
    }
}
