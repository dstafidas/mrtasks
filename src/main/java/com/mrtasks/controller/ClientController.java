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
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.regex.Pattern;
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

    // Email regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    // Phone regex pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$"
    );

    private String validateClientDto(ClientDto clientDto) {
        if (clientDto.getEmail() != null && !clientDto.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(clientDto.getEmail()).matches()) {
                return "clients.error.invalid.email";
            }
        }
        if (clientDto.getPhone() != null && !clientDto.getPhone().isEmpty()) {
            if (!PHONE_PATTERN.matcher(clientDto.getPhone()).matches()) {
                return "clients.error.invalid.phone";
            }
        }
        return null;
    }

    // Rest of the methods remain the same, only updating error message references
    @GetMapping
    public String listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Authentication auth,
            HttpServletRequest request) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        boolean canSearchClients = rateLimitConfig.canSearchClients(auth.getName(), request.getRemoteAddr());
        PageDto<ClientDto> clientPageDto = new PageDto<>();

        if (!canSearchClients) {
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
            Authentication auth,
            HttpServletRequest request) {
        boolean canSearchClients = rateLimitConfig.canSearchClients(auth.getName(), request.getRemoteAddr());
        if (!canSearchClients) {
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
            Authentication auth,
            HttpServletRequest request) {
        // Rate limiting
        boolean canCreateClient = rateLimitConfig.canCreateClient(auth.getName(), request.getRemoteAddr());
        if (!canCreateClient) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("error.rate.limit.client");
        }

        String validationError = validateClientDto(clientDto);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(validationError);
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
    public ResponseEntity<?> updateClient(
            @PathVariable Long id,
            @ModelAttribute ClientDto clientDto,
            Authentication auth) {
        String validationError = validateClientDto(clientDto);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(validationError);
        }

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

    @GetMapping("/clients/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Client client = clientRepository.findByIdAndUser(id, user);

        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(client);
    }
}