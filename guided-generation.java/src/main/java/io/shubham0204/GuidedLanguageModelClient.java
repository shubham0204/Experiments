/*
 * Copyright 2025 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GuidedLanguageModelClient {

    private final String roleDescription;
    private final String host;
    private final int port;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;

    public GuidedLanguageModelClient(String roleDescription, String host, int port) {
        this.roleDescription = roleDescription;
        this.host = host;
        this.port = port;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        var builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .with(new JakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));
        this.schemaGenerator = new SchemaGenerator(builder.build());
    }

    public <T> T respond(String text, Class<T> responseType) {
        try {
            var httpRequest = buildRequest(text, responseType);
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("API request failed with status: " +
                        response.statusCode() + ", body: " + response.body());
            }

            String responseBody = response.body();
            ChatCompletionResponse chatResponse = objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            String content = chatResponse.choices.getFirst().content;

            return objectMapper.readValue(content, responseType);
        } catch (IOException e) {
            throw new RuntimeException("Network error while communicating with language model API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request was interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during API call", e);
        }
    }

    private <T> HttpRequest buildRequest(String text, Class<T> responseType) throws JsonProcessingException {
        var schema = this.schemaGenerator.generateSchema(responseType);
        var prompt = buildSystemPrompt(schema.toPrettyString(), responseType);
        CompletionRequest request = new CompletionRequest(
                List.of(
                        new Message("system", prompt),
                        new Message("user", text)
                ),
                schema
        );
        String jsonPayload = this.objectMapper.writeValueAsString(request);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s:%d/v1/chat/completions", host, port)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(60));

        return requestBuilder.build();
    }

    private <T> String buildSystemPrompt(String schema, Class<T> generateType) {
        var fieldDescriptions = getFieldNLDescriptions(generateType);
        var classDescription = getClassDescription(generateType);
        return """
                %s
                You must strictly adhere to the JSON schema provided.
                Do not include any explanations or additional text outside the JSON structure.
                
                The JSON schema is as follows:
                
                %s
                
                The class is described as:
                
                %s
                
                The fields are described as:
                
                %s
                
                Ensure that the output is valid JSON and matches the schema exactly.
                """.formatted(
                this.roleDescription,
                schema,
                classDescription,
                String.join("\n", fieldDescriptions)
        );
    }

    /**
     * Retrieves the description of the class from the @Guide annotation.
     * If the class is not annotated with @Guide or has an empty description,
     * an IllegalArgumentException is thrown.
     */
    private <T> String getClassDescription(Class<T> generateType) throws IllegalArgumentException {
        if (!generateType.isAnnotationPresent(Guide.class)) {
            throw new IllegalArgumentException("The class " + generateType.getName() + " is not annotated with @Generable");
        }

        var classGenerationAnnotation = generateType.getAnnotation(Guide.class);
        if (classGenerationAnnotation.description().trim().isEmpty()) {
            throw new IllegalArgumentException("The class " + generateType.getName() + " has no description, but is annotated with @Generable");
        }
        return classGenerationAnnotation.description();
    }

    /**
     * Retrieves the natural language (NL) descriptions of all fields in the given class
     * that are annotated with @Guide. If any annotated field has an empty description,
     * an IllegalArgumentException is thrown.
     */
    private <T> List<String> getFieldNLDescriptions(Class<T> generateType) throws IllegalArgumentException {
        var guideAnnotatedFields = Arrays.stream(generateType.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Guide.class))
                .toList();
        guideAnnotatedFields.forEach(field -> {
            var guideAnnotation = field.getAnnotation(Guide.class);
            if (guideAnnotation.description().trim().isEmpty()) {
                throw new IllegalArgumentException("The field " + field.getName() + " has no description, but is annotated with @Guide");
            }
        });
        return guideAnnotatedFields.stream().map(this::getFieldNLDescription).toList();
    }

    /**
     * Creates the natural language (NL) description of the given field. For instance,
     */
    private String getFieldNLDescription(Field field) {
        return String.format(
                """
                Field '%s' has type of '%s' and is described as '%s'
                """.trim(),
                field.getName(),
                field.getType().getName(),
                field.getAnnotation(Guide.class).description()
        );
    }


    // Records for request payload

    private record CompletionRequest(
            List<Message> messages,
            @JsonProperty("json_schema")
            Object jsonSchema
    ) {}

    private record Message(String role, String content) {}


    // Records for response payload

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public String content;
        public int index;

        @JsonProperty("message")
        public void unpackContent(Map<String, Object> content) {
            this.content = (String) content.get("content");
        }
    }
}