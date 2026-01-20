package com.example.projet.controller;



import org.springframework.web.bind.annotation.*;

 

@RestController
@RequestMapping("/api")
public class TestController {
    
   
    
    // Test endpoint
    @GetMapping("/test")
    public String test() {
        return "✅ Spring Boot fonctionne! Le controller est opérationnel.";
    }
   
}
