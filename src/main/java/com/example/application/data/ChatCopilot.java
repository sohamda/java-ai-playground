package com.example.application.data;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;

public class ChatCopilot {
    private String chatId;
    private Kernel chatAssistantKernel;
    private ChatHistory chatHistory;
    public ChatCopilot(String chatId, Kernel chatAssistantKernel) {
        this.chatId = chatId;
        this.chatAssistantKernel = chatAssistantKernel;

        this.chatHistory =  this.chatAssistantKernel.getService("chatAssistant", ChatCompletion.class).createNewChat("""
           You are a customer chat support agent of an airline named "Funnair".",
           Respond in a friendly, helpful, and joyful manner.
           Before providing information about a booking or cancelling a booking,
           you MUST always get the following information from the user:
           booking number, customer first name and last name.
           Before changing a booking you MUST ensure it is permitted by the terms.
           If there is a charge for the change, you MUST ask the user to consent before proceeding.
           Today is {{current_date}}.
           """);
    }

    public String getChatId() {
        return chatId;
    }
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public ChatHistory getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(ChatHistory chatHistory) {
        this.chatHistory = chatHistory;
    }

    public Kernel getChatAssistantKernel() {
        return chatAssistantKernel;
    }

    public void setChatAssistantKernel(Kernel chatAssistantKernel) {
        this.chatAssistantKernel = chatAssistantKernel;
    }
}
