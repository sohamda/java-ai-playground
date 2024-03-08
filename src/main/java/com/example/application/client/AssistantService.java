package com.example.application.client;


import com.example.application.data.ChatCopilot;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.chatcompletion.ChatRequestSettings;
import com.microsoft.semantickernel.planner.actionplanner.ActionPlanner;
import com.microsoft.semantickernel.planner.actionplanner.Plan;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlanner;
import com.microsoft.semantickernel.textcompletion.CompletionRequestSettings;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.BrowserCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;


@BrowserCallable
@AnonymousAllowed
public class AssistantService {

    Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final Kernel chatAssistantKernel;
    private final ActionPlanner actionPlanner;
    private final Kernel kernelWithSkills;
    private final Map<String, ChatCopilot> chatsInMemory;


    public AssistantService(Kernel chatAssistantKernel, Kernel kernelWithSkills, ActionPlanner actionPlanner) {

        this.chatAssistantKernel = chatAssistantKernel;
        this.kernelWithSkills = kernelWithSkills;
        this.actionPlanner = actionPlanner;
        this.chatsInMemory = new HashMap<>();
    }

    public Flux<String> chat(String chatId, String userMessage) {
        log.debug("chatid {} and usermsg {}", chatId, userMessage);

        if(!this.chatsInMemory.containsKey(chatId)) {
            ChatCopilot chatCopilot = new ChatCopilot(chatId, chatAssistantKernel);
            this.chatsInMemory.put(chatId, chatCopilot);
        }

        ChatCopilot chatCopilot = this.chatsInMemory.get(chatId);

        chatCopilot.getChatHistory().addMessage(ChatHistory.AuthorRoles.User, userMessage);

        String actOnUserMessage = actOnUserMessage(userMessage);
        if(!actOnUserMessage.equalsIgnoreCase(userMessage)) {
            chatCopilot.getChatHistory().addMessage(ChatHistory.AuthorRoles.System, actOnUserMessage);
        }
        return this.chatAssistantKernel.getService("chatAssistant", ChatCompletion.class)
                .generateMessageStream(chatCopilot.getChatHistory(), new ChatRequestSettings(new CompletionRequestSettings()));

       // return chatCopilot.getChatCompletion().generateMessageStream(chatCopilot.getChatHistory(),new ChatRequestSettings(new CompletionRequestSettings()));
        /*Mono<String> messageAsync = chatKernel.generateMessageAsync(chatHistory, new ChatRequestSettings(new CompletionRequestSettings()));

        chatHistory.addMessage(ChatHistory.AuthorRoles.Assistant, messageAsync.block());
        return messageAsync;*/
    }

    private String actOnUserMessage(String userMessage) {
        Plan plan = actionPlanner.createPlanAsync(userMessage).block();
        log.debug("Plan created for {} is {}", userMessage, plan.toPlanString());

        return plan.invokeAsync(userMessage).block().getResult();
    }
}
