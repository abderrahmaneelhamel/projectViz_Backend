package com.javatechie.Service;

import com.javatechie.Exceptions.UpgradeplanException;
import com.javatechie.auth.user.User;
import com.javatechie.auth.user.UserRepository;
import com.javatechie.dto.ChatGPTPrompt;
import com.javatechie.dto.ChatGPTRequest;
import com.javatechie.dto.ChatGptResponse;
import com.javatechie.entity.Conversation;
import com.javatechie.entity.UserResponse;
import com.javatechie.repository.ConversationRepository;
import com.javatechie.repository.UserResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatService {
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final UserResponseRepository userResponseRepository;
    private final RestTemplate template;

    @Autowired
    public ChatService(RestTemplate template, UserRepository userRepository, ConversationRepository conversationRepository, UserResponseRepository userResponseRepository){
        this.template = template;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.userResponseRepository = userResponseRepository;
    }

    private enum ConversationState {
        ENHANCE_INPUT,
        PROJECT_ANALYSIS,
        TECH_RECOMMENDATION,
        UML_GENERATION
    }

    private ConversationState currentState = ConversationState.ENHANCE_INPUT;
    private String enhancedInput;
    private String projectAnalysisResponse;
    private String techRecommendationResponse;
    private String ClassUmlGenerationResponse;
    private String SequenceUmlGenerationResponse;
    private String UseCaseUmlGenerationResponse;
    private UserResponse finalResponse;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;


    public UserResponse processProjectDescription(ChatGPTPrompt chatGPTPrompt)  throws UpgradeplanException {
    User user = userRepository.findById(chatGPTPrompt.getUserId()).orElse(null);
    if (user != null) {
        int count = conversationRepository.findByUserIdLite(user.getId()).size();
        int allowedConversations= user.getPlan().getAllowedConversations();
            if (count < allowedConversations || allowedConversations == -1){
                String projectDescription = chatGPTPrompt.getPrompt();
                CompletableFuture<String> enhancedInput = CompletableFuture.supplyAsync(() -> enhanceUserInput(projectDescription))
                        .thenApply(response -> {
                            this.enhancedInput = response;
                            currentState = ConversationState.PROJECT_ANALYSIS;
                            return response;
                        });
                CompletableFuture<String> firstCall = CompletableFuture.supplyAsync(() -> processProjectAnalysis(this.enhancedInput))
                        .thenApply(response -> {
                            this.projectAnalysisResponse = response;
                            currentState = ConversationState.TECH_RECOMMENDATION;
                            return response;
                        });

                CompletableFuture<String> secondCall = firstCall.thenApply(response -> processTechRecommendation(this.enhancedInput))
                        .thenApply(response -> {
                            this.techRecommendationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });

                CompletableFuture<String> thirdCall = secondCall.thenApply(response -> processClassUMLGeneration(this.enhancedInput))
                        .thenApply(response -> {
                            this.ClassUmlGenerationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });
                CompletableFuture<String> fourthCall = thirdCall.thenApply(response -> processSequenceUMLGeneration(this.enhancedInput))
                        .thenApply(response -> {
                            this.SequenceUmlGenerationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });
                CompletableFuture<String> fifthCall = fourthCall.thenApply(response -> processUseCaseUMLGeneration(this.enhancedInput))
                        .thenApply(response -> {
                            this.UseCaseUmlGenerationResponse = response;
                            currentState = ConversationState.PROJECT_ANALYSIS;
                            this.finalResponse = new UserResponse(projectAnalysisResponse,techRecommendationResponse,ClassUmlGenerationResponse,SequenceUmlGenerationResponse,UseCaseUmlGenerationResponse);
                            return response;
                        });

                CompletableFuture<Void> allCalls = CompletableFuture.allOf(enhancedInput,firstCall, secondCall, thirdCall, fourthCall, fifthCall);

                allCalls.join();

                addUserToConversation(this.finalResponse,chatGPTPrompt,user);
                return finalResponse;
            }else{
                throw new UpgradeplanException("You have reached the maximum number of conversations for this plan");
            }
        }else{
            return null;
        }
    }

    private void addUserToConversation(UserResponse userResponse, ChatGPTPrompt chatGPTPrompt, User User) {
        UserResponse Response = userResponseRepository.save(userResponse);
        System.out.println(Response);
        conversationRepository.save(new Conversation(User,chatGPTPrompt.getPrompt(),Response));
    }

    private String enhanceUserInput(String userInput) {
        String enhancementPrompt = "As an AI language model, I need you to enhance and clarify the following project description. Provide additional details, rephrase sentences for clarity, and ensure the overall coherence and completeness of the description. Here is the project description:\n\n" + userInput;
        String enhancedInput = sendPromptToAI(enhancementPrompt);
        return enhancedInput;
    }

    public String processProjectAnalysis(String projectDescription) {
        String analysisPrompt = "Imagine yourself as an experienced Software Engineer tasked with conducting a thorough analysis of a project. Your primary focus is to extract and understand the core idea or concept behind the project described below. Provide detailed insights into its significance, objectives, potential challenges, and strategies for overcoming them. Additionally, include any relevant supporting details such as required resources, timelines, and potential collaborators necessary for successful implementation. Your analysis should be comprehensive, informative, and well-structured, offering a clear understanding of the project's main components and objectives.\n\n" +
                projectDescription;
        return sendPromptToAI(analysisPrompt);
    }

    public String processTechRecommendation(String techRecommendation) {
        String techPrompt = "Act as a Software Engineer and please provide tech Recommendations for this project: " + techRecommendation;
        return sendPromptToAI(techPrompt);
    }

    public String processClassUMLGeneration(String umlGeneration) {

        String umlPrompt = "As an Experienced Software Engineer and UML Architect, your task is to design a detailed UML class diagram for our database architecture. Ensure the diagram reflects database entities, attributes, methods, and relationships accurately. Incorporate SOLID principles, design patterns, and domain-driven design concepts to create a robust and maintainable diagram. Provide clear annotations and comments in the PlantUML representation to convey your expertise and forward-thinking approach, make it in a plantUML format for this project : " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);

        // Find the index of @startuml
        int startUMLIndex = umlResponse.indexOf("@startuml");

        // Find the index of @enduml
        int endUMLIndex = umlResponse.indexOf("@enduml");

        // Check if both start and end markers are present
        if (startUMLIndex != -1 && endUMLIndex != -1) {
            // Extract the portion between @startuml and @enduml (inclusive)
            String umlScript = umlResponse.substring(startUMLIndex, endUMLIndex + "@enduml".length());

            // Generate PNG image from UML description
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(umlScript);
            try {
                // Write the image to "pngStream"
                reader.generateImage(pngStream, new FileFormatOption(FileFormat.PNG,false));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Convert the PNG stream to a Base64-encoded string
            String pngBase64 = Base64.getEncoder().encodeToString(pngStream.toByteArray());

            return pngBase64;
        } else {
            // Handle the case where @startuml or @enduml is missing
            return "Error: @startuml or @enduml not found in the UML response.";
        }
    }

    public String processSequenceUMLGeneration(String umlGeneration) {

        String umlPrompt = "In your role as a Software Architect and System Designer, your objective is to create a precise UML sequence diagram depicting system interactions. Ensure the diagram captures all system components, interactions, and message flows accurately. Consider scenarios like asynchronous communication, error handling, concurrency, and alternative paths to provide a comprehensive view. Present the diagram in a concise and readable PlantUML format with detailed lifelines and message annotations, reflecting your methodical approach and attention to detail, make it in a plantUML format for this project : " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);

        // Find the index of @startuml
        int startUMLIndex = umlResponse.indexOf("@startuml");

        // Find the index of @enduml
        int endUMLIndex = umlResponse.indexOf("@enduml");

        // Check if both start and end markers are present
        if (startUMLIndex != -1 && endUMLIndex != -1) {
            // Extract the portion between @startuml and @enduml (inclusive)
            String umlScript = umlResponse.substring(startUMLIndex, endUMLIndex + "@enduml".length());

            // Generate PNG image from UML description
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(umlScript);
            try {
                // Write the image to "pngStream"
                reader.generateImage(pngStream, new FileFormatOption(FileFormat.PNG,false));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Convert the PNG stream to a Base64-encoded string
            String pngBase64 = Base64.getEncoder().encodeToString(pngStream.toByteArray());

            return pngBase64;
        } else {
            // Handle the case where @startuml or @enduml is missing
            return "Error: @startuml or @enduml not found in the UML response.";
        }
    }


    public String processUseCaseUMLGeneration(String umlGeneration) {

        String umlPrompt = "In your role as a Software Engineer and Requirements Analyst, your objective is to develop a comprehensive UML use case diagram encompassing all project functionalities and user interactions. Ensure the diagram clearly identifies actors, use cases, relationships, and alternate flows. Craft a structured and intuitive PlantUML diagram that reflects your analytical mindset and commitment to delivering a user-centric solution, make it in a plantUML format for this project : " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);

        // Find the index of @startuml
        int startUMLIndex = umlResponse.indexOf("@startuml");

        // Find the index of @enduml
        int endUMLIndex = umlResponse.indexOf("@enduml");

        // Check if both start and end markers are present
        if (startUMLIndex != -1 && endUMLIndex != -1) {
            // Extract the portion between @startuml and @enduml (inclusive)
            String umlScript = umlResponse.substring(startUMLIndex, endUMLIndex + "@enduml".length());

            // Generate PNG image from UML description
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(umlScript);
            try {
                // Write the image to "pngStream"
                reader.generateImage(pngStream, new FileFormatOption(FileFormat.PNG,false));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Convert the PNG stream to a Base64-encoded string
            String pngBase64 = Base64.getEncoder().encodeToString(pngStream.toByteArray());

            return pngBase64;
        } else {
            // Handle the case where @startuml or @enduml is missing
            return "Error: @startuml or @enduml not found in the UML response.";
        }
    }

    private String sendPromptToAI(String prompt) {
        ChatGPTRequest request = new ChatGPTRequest(model, prompt);
        ResponseEntity<ChatGptResponse> responseEntity = template.postForEntity(apiURL, request, ChatGptResponse.class);
        return responseEntity.getBody().getChoices().get(0).getMessage().getContent();
    }
}
