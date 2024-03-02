# AI powered expert system demo

This app shows how you can use LangChain4j to build an AI-powered system that:

- Has access to terms and conditions (retrieval augmented generation, RAG)
- Can access tools (Java methods) to perform actions
- Uses an LLM to interact with the user

All credit for the example goes to [LangChain4j](https://github.com/langchain4j/langchain4j-examples/tree/main/spring-boot-example), 
this demo simply expands on it by adding a React UI and a database.

## Requirements
- Java 17+
- Azure OpenAI details in `application.properties`

````properties
azure.openai.endpoint = https://blala.openai.azure.com/
azure.openai.key = KEY
azure.deployment.name = DEPLOYMENT_NAME
````

## Running
Run the app by running `Application.java` in your IDE or `mvn` in the command line.