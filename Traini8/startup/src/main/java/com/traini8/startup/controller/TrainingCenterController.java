package com.traini8.startup.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.traini8.startup.entity.TrainingCenter;
import com.traini8.startup.repository.TrainingCenterRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/training-centers")
@Validated
public class TrainingCenterController {

    private final TrainingCenterRepository repository;

    public TrainingCenterController(TrainingCenterRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/add")
    public ResponseEntity<?> createTrainingCenter(@Valid @RequestBody TrainingCenter trainingCenter) {
        try {
            // Ensure createdOn is always set by the server (ignoring user input)
            trainingCenter.setCreatedOn(Instant.now().getEpochSecond());

            TrainingCenter savedCenter = repository.save(trainingCenter);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCenter);

        } 
        //Check duplicate enteries
        catch (DataIntegrityViolationException ex) {
            String msg = ex.getRootCause().getMessage();
            Map<String, String> errors = Map.of(
                    "center_name", "Center Name already exists.",
                    "center_code", "Center Code must be unique.",
                    "contact_email", "This email is already registered.",
                    "contact_phone", "This phone number is already in use.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errors.entrySet().stream()
                            .filter(e -> msg.contains(e.getKey()))
                            .findFirst()
                            .map(e -> Map.of(e.getKey().split("_")[0], e.getValue()))
                            .orElse(Map.of("error", "Duplicate entry detected.")));

        } 
        //If any another type of exception is occur or internal server error
        catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong: " + ex.getMessage()));
        }
    }
    
    //Check all existing validation if present it automcatically occurs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @GetMapping("/get")
    public ResponseEntity<List<TrainingCenter>> getAllTrainingCenters() {
        List<TrainingCenter> centers = repository.findAll();
        return ResponseEntity.ok(centers); // Returns empty list if no centers are found
    }
}
