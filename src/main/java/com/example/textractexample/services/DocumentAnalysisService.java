package com.example.textractexample.services;

import com.example.textractexample.aws.AwsTextractClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.model.*;
import java.util.*;

@Service
@AllArgsConstructor
public class DocumentAnalysisService {

    private AwsTextractClient textractClient;

    public Map<String,String> AnalyzeDocument(MultipartFile documentFile, List<String> queries) throws Exception {

        //Building queries
        List<Query> textractQueries = new ArrayList<>();
        for (String query : queries) {
            textractQueries.add(Query.builder()
                    .text(query)
                    .build());
        }

        //Configuring textract analysis
        AnalyzeDocumentRequest documentAnalysisRequest = AnalyzeDocumentRequest
                .builder().document(Document.builder()
                        .bytes(SdkBytes.fromByteArray(documentFile.getBytes()))
                        .build())
                .featureTypesWithStrings("QUERIES")
                .queriesConfig(QueriesConfig
                        .builder()
                        .queries(textractQueries)
                        .build())
                .build();

        //Calling textract
        AnalyzeDocumentResponse response = textractClient.getClient().analyzeDocument(documentAnalysisRequest);

        //Getting queries responses
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

        return responseMap;
    }
}
