package com.example.textractexample.controllers;

import com.example.textractexample.services.DocumentAnalysisService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/document")
@AllArgsConstructor
public class DocumentAnalysisController {
    DocumentAnalysisService analyzerService;
    @PostMapping(value= "/analyze", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Object> handleFileUpload(@RequestPart("file") MultipartFile file, @RequestParam("query") List<String> queries) throws Exception {
        if (file != null) {
            return new ResponseEntity<>(analyzerService.AnalyzeDocument(file, queries), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
