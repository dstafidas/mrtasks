package com.mrtasks.controller;

import com.mrtasks.model.Client;
import com.mrtasks.model.User;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @GetMapping("/clients")
    public String listClients(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Client> clients = clientRepository.findByUser(user);
        model.addAttribute("clients", clients);
        model.addAttribute("newClient", new Client());
        return "clients";
    }

    @PostMapping("/clients")
    public String addClient(@ModelAttribute Client client, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        client.setUser(user);
        clientRepository.save(client);
        return "redirect:/clients";
    }

    @GetMapping("/clients/{id}")
    @ResponseBody
    public ResponseEntity<Client> getClient(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client client = clientRepository.findByIdAndUser(id, user);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @PutMapping("/clients/{id}")
    @ResponseBody
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @ModelAttribute Client client, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client existingClient = clientRepository.findByIdAndUser(id, user);
        if (existingClient == null) {
            return ResponseEntity.notFound().build();
        }
        existingClient.setName(client.getName());
        existingClient.setEmail(client.getEmail());
        existingClient.setPhone(client.getPhone());
        existingClient.setAddress(client.getAddress());
        existingClient.setTaxId(client.getTaxId());
        clientRepository.save(existingClient);
        return ResponseEntity.ok(existingClient);
    }

    @DeleteMapping("/clients/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteClient(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client client = clientRepository.findByIdAndUser(id, user);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        clientRepository.delete(client);
        return ResponseEntity.noContent().build();
    }
}