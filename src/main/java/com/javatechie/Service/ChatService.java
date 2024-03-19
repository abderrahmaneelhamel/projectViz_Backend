package com.javatechie.Service;

import com.javatechie.Exceptions.UpgradeplanException;
import com.javatechie.auth.user.User;
import com.javatechie.auth.user.UserRepository;
import com.javatechie.dto.ChatGPTPrompt;
import com.javatechie.dto.ChatGPTRequest;
import com.javatechie.dto.ChatGptResponse;
import com.javatechie.entity.Client;
import com.javatechie.entity.Conversation;
import com.javatechie.entity.UserResponse;
import com.javatechie.repository.ClientRepository;
import com.javatechie.repository.ConversationRepository;
import com.javatechie.repository.UserResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatService {
    private final ClientRepository clientRepository;
    private final ConversationRepository conversationRepository;
    private final UserResponseRepository userResponseRepository;
    private final RestTemplate template;

    @Autowired
    public ChatService(RestTemplate template, ClientRepository clientRepository, ConversationRepository conversationRepository, UserResponseRepository userResponseRepository){
        this.template = template;
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
        this.userResponseRepository = userResponseRepository;
    }

    private enum ConversationState {
        PROJECT_ANALYSIS,
        TECH_RECOMMENDATION,
        UML_GENERATION
    }

    private ConversationState currentState = ConversationState.PROJECT_ANALYSIS;
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
    Client client = clientRepository.findById(chatGPTPrompt.getUserId()).orElse(null);
    if (client != null) {
        int count = conversationRepository.findByClientIdLite(client.getId()).size();
        int allowedConversations= client.getPlan().getAllowedConversations();
            if (count < allowedConversations || allowedConversations == -1){
                String projectDescription = chatGPTPrompt.getPrompt();
                CompletableFuture<String> firstCall = CompletableFuture.supplyAsync(() -> processProjectAnalysis(projectDescription))
                        .thenApply(response -> {
                            this.projectAnalysisResponse = response;
                            currentState = ConversationState.TECH_RECOMMENDATION;
                            return response;
                        });

                CompletableFuture<String> secondCall = firstCall.thenApply(response -> processTechRecommendation(projectDescription))
                        .thenApply(response -> {
                            this.techRecommendationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });

                CompletableFuture<String> thirdCall = secondCall.thenApply(response -> processClassUMLGeneration(projectDescription))
                        .thenApply(response -> {
                            this.ClassUmlGenerationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });
                CompletableFuture<String> fourthCall = thirdCall.thenApply(response -> processSequenceUMLGeneration(projectDescription))
                        .thenApply(response -> {
                            this.SequenceUmlGenerationResponse = response;
                            currentState = ConversationState.UML_GENERATION;
                            return response;
                        });
                CompletableFuture<String> fifthCall = fourthCall.thenApply(response -> processUseCaseUMLGeneration(projectDescription))
                        .thenApply(response -> {
                            this.UseCaseUmlGenerationResponse = response;
                            currentState = ConversationState.PROJECT_ANALYSIS;
                            this.finalResponse = new UserResponse(projectAnalysisResponse,techRecommendationResponse,ClassUmlGenerationResponse,SequenceUmlGenerationResponse,UseCaseUmlGenerationResponse);
                            return response;
                        });

                // Wait for all CompletableFuture to complete
                CompletableFuture<Void> allCalls = CompletableFuture.allOf(firstCall, secondCall, thirdCall, fourthCall, fifthCall);

                // Block and wait for completion
                allCalls.join();

                addUserToConversation(this.finalResponse,chatGPTPrompt,client);
                return finalResponse;
            }else{
                throw new UpgradeplanException("You have reached the maximum number of conversations for this plan");
            }
        }else{
            return null;
        }
    }

    private void addUserToConversation(UserResponse userResponse, ChatGPTPrompt chatGPTPrompt, Client client) {
        UserResponse Response = userResponseRepository.save(userResponse);
        System.out.println(client);
        conversationRepository.save(new Conversation(client,chatGPTPrompt.getPrompt(),Response));
    }

    public String processProjectAnalysis(String projectDescription) {
        String analysisPrompt = "Act as a Software Engineer and provide a comprehensive analysis of the project, focusing on the main subject and all relevant information crucial for its successful achievement. Your analysis should encompass detailed information about the main subject, including its significance, objectives, potential challenges, and strategies for overcoming them. Additionally, consider including relevant supporting details, such as resources, timelines, and potential collaborators, that are essential for the project's successful implementation. Your analysis should be thorough, informative, and well-organized, providing a holistic view of the project and its key components : " + projectDescription;
        return sendPromptToAI(analysisPrompt);
    }

    public String processTechRecommendation(String techRecommendation) {
        String techPrompt = "Act as a Software Engineer and please provide tech Recommendations for this project: " + techRecommendation;
        return sendPromptToAI(techPrompt);
    }

    public String processClassUMLGeneration(String umlGeneration) {

        String umlPrompt = "Act as a Software Engineer and a UML expert and make a good, complete and well though of UML class diagram for the database with all the attributes and the methods for this project, make it very professional and complex, make it in a plantUML format: " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);
        System.out.println(umlResponse);

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

        String umlPrompt = "Act as a Software Engineer and a UML expert and make a good, complete and well though of UML sequence diagram for this project, make it very professional and complex, make it in a plantUML format: " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);
        System.out.println(umlResponse);

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

        String umlPrompt = "Act as a Software Engineer and a UML expert and make a good, complete and well though of UML Use Case diagram for this project, make it very professional and complex, make it in a plantUML format: " + umlGeneration;
        String umlResponse = sendPromptToAI(umlPrompt);
        System.out.println(umlResponse);

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
        System.out.println(responseEntity.getBody().getChoices().get(0).getMessage().getContent());
        return responseEntity.getBody().getChoices().get(0).getMessage().getContent();
    }
}
