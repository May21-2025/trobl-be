package com.may21.trobl.general;


import com.may21.trobl._global.Message;
import com.may21.trobl._global.component.GlobalValues;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class Controller {

    @GetMapping("/hello")
    public ResponseEntity<Message> hello() {
        String message = "Welcome to the Trobl API!";
        String version = GlobalValues.getBEVersion();

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("version", version);
        response.put("status", "available");
        response.put("timestamp", LocalDateTime.now().toString());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


}