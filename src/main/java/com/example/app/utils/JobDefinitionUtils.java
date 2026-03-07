package com.example.app.utils;

import com.example.app.data.entity.Configuration;
import com.example.app.data.entity.JobManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobDefinitionUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serializeJobDefinition(JobManager jobManager) throws JsonProcessingException {
        return objectMapper.writeValueAsString(jobManager);
    }

    public static JobManager deserializeJobDefinition(String jobDefinitionString) throws JsonProcessingException {
        return objectMapper.readValue(jobDefinitionString, JobManager.class);
    }

    public static String serializeJobDefinition(Configuration configuration) throws JsonProcessingException {
        return objectMapper.writeValueAsString(configuration);
    }

    public static Configuration deserializeJobConfDefinition(String jobDefinitionString) throws JsonProcessingException {
        return objectMapper.readValue(jobDefinitionString, Configuration.class);
    }

}
