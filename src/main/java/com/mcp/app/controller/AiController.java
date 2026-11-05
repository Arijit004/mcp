package com.mcp.app.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    private ChatClient chatClient;

    //  ToolCallbackProvider is a Spring AI interface that supplies the list of tools (ToolCallbacks) available for the LLM to invoke during a conversation.
    //  Think of it as a tool registry, which tells Spring AI: "These are all the tools the model is allowed to use."
    public AiController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbackProvider) // this is deprecated as it is not required in latest version
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    //  localhost:8081/ai/chat?query={query}
    @PostMapping("/chat")
    public ResponseEntity<String> getAiResponse(@RequestParam("query") String query) {
        String response=chatClient.prompt(query).call().content();
        return ResponseEntity.ok(response);
    }
}


/*
    What is MCP :
        MCP (Model Context Protocol) is an open standard that allows AI models to communicate with external tools, data sources, and applications in a standardized way.
        Ex: Think of it like USB-C for AI.
        Before USB-C, every device needed a different cable. After USB-C, one standard cable which works on many devices.
        Similar for MCP. Before MCP, every AI application implemented tool calling differently. With MCP, every AI client can communicate with every MCP server using the same protocol.


    Why was MCP created :
        LLMs only know what's inside their training data. Suppose you ask "How many open tickets do I have?", the model doesn't know the answer. Someone has to run query in db, jira, serviceNow etc.
        Before MCP, every application had to write custom integration code (what we call "tool" calling).
        With MCP, we don't need to write custom integration code repeatedly.


    What problem does MCP solve :
        Suppose you build an AI assistant. It should access: Database, Redis, Jira, Github, Kubernetes, Docker, Local files etc.
        Without MCP, we maintain every integration/tools for every external tools (such as db, redis etc.) by yourself.
        With MCP, we can use MCP client to get MCP services (without writing integration code) from MCP servers. The client doesn't need to know how each system works internally.


    Key components :
        1. MCP Client: The MCP Client is the consumer of MCP services.
        It is responsible for Discovering available tools, Asking the server what tools exist, Calling tools, Receiving responses

        2. MCP Server: The MCP Server exposes tools. Think of it as a Tool Provider.
        Ex: FileSystem MCP Server contains searchFiles(), readFile(), writeFile(), deleteFile(). These are the available tools.
        Another Ex: GitHub MCP Server contains createIssue(), getPR(),listRepositories(), searchCode() tools.
        Client can call any of these as per requirement.

        3. Tools: Tools are nothing but callable functions. Every MCP server exposes one or more tools.

        4. Resources: Resources are data that can be read to gather information. Ex.: readme.md, database schema, api documentation, config files

        5. Transport Mechanism: The transport is simply the communication channel between the MCP Client and the MCP Server.
        Why it is needed : the client and server might be running in same machines or in different docker containers or in different servers or written in different languages
        Types :
            a. STDIO (Standard Input/Output): Client starts server as a child process and communicates through STDIN (Standard Input) and STDOUT (Standard Output).
                When to use: Local tools, Local filesystem, Desktop application, Development
            b. Streamable HTTP (Modern MCP): The client communicates with the server over HTTP while supporting streaming responses.
                When to use: Prefferred for modern distributed application
                Advantages: Remote servers, Kubernetes, Docker, Cloud deployment, Load balancing,Authentication, TLS
            c. SSE/Stream-Sent Events (Legacy): It allows the server to stream messages back to the client over a long-lived HTTP connection.
                However, Streamable HTTP has largely replaced SSE in the current MCP specification because it provides a cleaner and more flexible communication model.


    Execution flow :
        User query -> Spring AI -> Ollama (or any llm) -> Need a tool? (if yes)-> MCP Client -> GitHub MCP Server (or any other as per requirement) -> listIssues() (or any other tool as per requirement)
        -> JSON response returned by tool -> Ollama -> Natural language response

    Tool calling vs MCP :
        1. Tool Calling is the ability of an LLM to invoke functions. For tool calling, every function lives inside application and when model decides to call tool as per requirement, spring ai executes the function
        execution flow: user query -> chatClient -> ToolCallBack -> Java Method/tool -> Result -> llm
        2. MCP is a standardized protocol for discovering and invoking tools that may live outside (inside MCP server) your application.
        execution flow: user query -> chatClient -> MCP client -> MCP server -> Tool -> Result -> LLM
*/