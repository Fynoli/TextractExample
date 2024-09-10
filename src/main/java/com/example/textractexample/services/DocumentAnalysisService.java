package com.example.textractexample.services;

import com.example.textractexample.aws.AwsS3Client;
import com.example.textractexample.aws.AwsTextractClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@AllArgsConstructor
public class DocumentAnalysisService {

    private AwsTextractClient textractClient;
    private AwsS3Client s3Client;

    public Map<String,String> AnalyzeDocument(MultipartFile documentFile, List<String> queries) throws Exception {
        Path expenseTempDirPath = null;
        final String bucket = "finn-quippos-dev";

        // Generating a unique id to retrieve the file later
        String fileId = UUID.randomUUID().toString();

        // Creating the directory for caching expenses
        expenseTempDirPath = Paths.get("src/main/resources/documents").resolve(fileId);
        if (!Files.exists(expenseTempDirPath)) {
            Files.createDirectories(expenseTempDirPath);
        }
        Path filePath = expenseTempDirPath.resolve(Objects.requireNonNull(documentFile.getOriginalFilename()));

        // Saving the file into a temporal directory
        Files.write(filePath, documentFile.getBytes());

        // Sending the file to S3
        String s3DocumentObjectKey = "textract_input" + "/" + fileId + "/" + documentFile.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(s3DocumentObjectKey)
                .build();

        s3Client.getS3Client().putObject(putObjectRequest,filePath);

        List<Query> textractQueries = new ArrayList<>();
        for (String query : queries) {
            textractQueries.add(Query.builder()
                    .text(query)
                    .build());
        }

        //Starting textract analysis
        StartDocumentAnalysisRequest documentAnalysisRequest = StartDocumentAnalysisRequest
                .builder()
                .documentLocation(
                        DocumentLocation
                                .builder()
                                .s3Object(S3Object.builder()
                                        .bucket(bucket)
                                        .name(s3DocumentObjectKey).build())
                                .build()
                )
                .featureTypesWithStrings("QUERIES")
                .queriesConfig(QueriesConfig
                        .builder()
                        .queries(textractQueries)
                        .build())
                .outputConfig(OutputConfig
                        .builder()
                        .s3Bucket(bucket)
                        .s3Prefix("textract_input")
                        .build())
                .build();

        var textractStartTime = System.currentTimeMillis();
        StartDocumentAnalysisResponse startDocumentAnalysisResponse = textractClient.getClient()
                .startDocumentAnalysis(documentAnalysisRequest);

        //Wait for Textract to finalize and getting the response
        boolean finished = false;
        String status = "" ;

        GetDocumentAnalysisRequest analysisRequest = GetDocumentAnalysisRequest.builder()
                .jobId(startDocumentAnalysisResponse.jobId())
                .build();
        GetDocumentAnalysisResponse response = null;

        while (!finished) {
            response = textractClient.getClient().getDocumentAnalysis(analysisRequest);
            status = response.jobStatus().toString();

            if (status.equals("SUCCEEDED"))
                finished = true;
            else {
                Thread.sleep(250);
            }
        }
        var textractEndTime = System.currentTimeMillis();

        Map<String,String> queriesResultsMap = new HashMap<>();
        Map<String,String> queriesMap = new HashMap<>();
        Map<String,String> queriesRelationMap = new HashMap<>();
        Map<String,String> responseMap = new HashMap<>();
        List<String> queriesIdsList = new ArrayList<>();

        for (Block block : response.blocks()) {
            if (block.blockType().equals(BlockType.QUERY)){
                for (var relation : block.relationships()) {
                    if(relation.type().equals(RelationshipType.ANSWER)){
                        queriesRelationMap.put(block.id(),relation.ids().get(0));
                        break;
                    }
                }
                queriesIdsList.add(block.id());
                queriesMap.put(block.id(),block.query().text());
            }
            if (block.blockType().equals(BlockType.QUERY_RESULT)) {
                queriesResultsMap.put(block.id(), block.text());
            }
        }

        for (var queryId : queriesIdsList) {
            responseMap.put(queriesMap.get(queryId),queriesResultsMap.get(queriesRelationMap.get(queryId)));
        }

        Float textractTime = (textractEndTime-textractStartTime)/1000f;
        responseMap.put("textractTime",textractTime+"s");

        return responseMap;
    }
}
