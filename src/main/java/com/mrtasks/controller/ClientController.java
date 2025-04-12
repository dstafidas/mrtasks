package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.model.Client;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.dto.ClientDto;
import com.mrtasks.model.dto.PageDto;
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RateLimitConfig rateLimitConfig;
    private final UserProfileRepository userProfileRepository;
    private final DtoMapper dtoMapper;

    @GetMapping
    public String listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        // Rate limiting
        Bucket bucket = rateLimitConfig.getClientSearchBucket(auth.getName()); // Reusing client search bucket for consistency
        PageDto<ClientDto> clientPageDto = new PageDto<>();

        if (!bucket.tryConsume(1)) {
            // Rate limit exceeded, return empty page with error message
            clientPageDto.setContent(new java.util.ArrayList<>());
            clientPageDto.setPageNumber(page);
            clientPageDto.setPageSize(size);
            clientPageDto.setTotalPages(0);
            clientPageDto.setTotalElements(0L);
            model.addAttribute("errorMessage", "error.rate.limit.client.search");
        } else {
            // Fetch clients if rate limit allows
            Pageable pageable = PageRequest.of(page, size);
            Page<Client> clientPage = clientRepository.findByUser(user, pageable);

            clientPageDto.setContent(clientPage.getContent().stream()
                    .map(dtoMapper::toClientDto)
                    .collect(Collectors.toList()));
            clientPageDto.setPageNumber(clientPage.getNumber());
            clientPageDto.setPageSize(size);
            clientPageDto.setTotalPages(clientPage.getTotalPages());
            clientPageDto.setTotalElements(clientPage.getTotalElements());
        }

        model.addAttribute("clients", clientPageDto);
        model.addAttribute("search", null);
        model.addAttribute("newClient", new ClientDto());
        return "clients";
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication auth) {
        // Rate limiting
        Bucket bucket = rateLimitConfig.getClientSearchBucket(auth.getName());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("error.rate.limit.client.search");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        Pageable pageable = PageRequest.of(page, size);
        Page<Client> clientPage;

        if (search != null && !search.trim().isEmpty()) {
            clientPage = clientRepository.findByUserAndNameContainingIgnoreCase(user, search, pageable);
        } else {
            clientPage = clientRepository.findByUser(user, pageable);
        }

        PageDto<ClientDto> clientPageDto = new PageDto<>();
        clientPageDto.setContent(clientPage.getContent().stream()
                .map(dtoMapper::toClientDto)
                .collect(Collectors.toList()));
        clientPageDto.setPageNumber(clientPage.getNumber());
        clientPageDto.setPageSize(size);
        clientPageDto.setTotalPages(clientPage.getTotalPages());
        clientPageDto.setTotalElements(clientPage.getTotalElements());

        return ResponseEntity.ok(clientPageDto);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> addClient(
            @ModelAttribute ClientDto clientDto,
            Authentication auth) {
        // Rate limiting
        Bucket bucket = rateLimitConfig.getClientCreationBucket(auth.getName());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("error.rate.limit.client");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isEmailVerified = userProfileRepository.findByUser(user)
                .map(UserProfile::isEmailVerified)
                .orElse(false);

        // Check email verification and client limit
        if (!isEmailVerified) {
            long clientCount = clientRepository.countByUser(user);
            if (clientCount >= 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("error.client.limit.unverified");
            }
        }

        Client client = new Client();
        client.setName(clientDto.getName());
        client.setEmail(clientDto.getEmail());
        client.setPhone(clientDto.getPhone());
        client.setAddress(clientDto.getAddress());
        client.setTaxId(clientDto.getTaxId());
        client.setUser(user);
        clientRepository.save(client);

        return ResponseEntity.ok(dtoMapper.toClientDto(client));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ClientDto> getClient(
            @PathVariable Long id,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client client = clientRepository.findByIdAndUser(id, user);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dtoMapper.toClientDto(client));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ClientDto> updateClient(
            @PathVariable Long id,
            @ModelAttribute ClientDto clientDto,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client existingClient = clientRepository.findByIdAndUser(id, user);
        if (existingClient == null) {
            return ResponseEntity.notFound().build();
        }
        existingClient.setName(clientDto.getName());
        existingClient.setEmail(clientDto.getEmail());
        existingClient.setPhone(clientDto.getPhone());
        existingClient.setAddress(clientDto.getAddress());
        existingClient.setTaxId(clientDto.getTaxId());
        clientRepository.save(existingClient);
        return ResponseEntity.ok(dtoMapper.toClientDto(existingClient));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteClient(
            @PathVariable Long id,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client client = clientRepository.findByIdAndUser(id, user);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        if (taskRepository.existsByClient(client)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Error-Message", "clients.error.delete.associatedTasks")
                    .build();
        }

        clientRepository.delete(client);
        return ResponseEntity.noContent().build();
    }
}