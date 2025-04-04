package ufsc.br.epibuilder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ufsc.br.epibuilder.model.Epitope;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.service.EpitopeService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

@RestController
public class EpitopeController {

    private final EpitopeService epitopeService;

    public EpitopeController(EpitopeService epitopeService) {
        this.epitopeService = epitopeService;
    }

    @PostMapping("/new")
    public ResponseEntity<EpitopeTaskData> newEpitopeTask(@RequestBody EpitopeTaskData taskData) {
        try {
            /* ProcessBuilder processBuilder = new ProcessBuilder("nextflow", "run", "pipeline_script.nf", "--input", taskData.getInputData());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();

        //taskData.setOutputData(output.toString());*/
            return ResponseEntity.ok(taskData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/results")
    public ResponseEntity<List<Epitope>> getAllEpitopes() {
        List<Epitope> epitopes = epitopeService.findAll();
        return ResponseEntity.ok(epitopes);
    }

}
