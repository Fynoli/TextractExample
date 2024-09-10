package com.example.textractexample.aws;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;

@Service
public class AwsTextractClient {

    private TextractClient textractClient;
    public AwsTextractClient() {
        textractClient = TextractClient
                .builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public TextractClient getClient(){
        return textractClient;
    }
}
