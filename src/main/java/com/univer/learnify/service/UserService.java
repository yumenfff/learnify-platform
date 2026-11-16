package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateUserRequest;
import com.univer.learnify.dto.request.UpdateUserRequest;
import com.univer.learnify.entity.Profile;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email " + email + " уже существует");
        }
        User user = new User(request.name(), email, request.role());
        if (request.bio() != null || request.avatarUrl() != null) {
            user.setProfile(new Profile(request.bio(), request.avatarUrl()));
        }
        User saved = userRepository.save(user);
        log.info("Создан пользователь id={}, role={}", saved.getId(), saved.getRole());
        return saved;
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
    }

    public List<User> getUsers(Role role) {
        return role == null ? userRepository.findAll() : userRepository.findByRole(role);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.bio() != null || request.avatarUrl() != null) {
            Profile profile = user.getProfile();
            if (profile == null) {
                profile = new Profile();
                user.setProfile(profile);
            }
            if (request.bio() != null) {
                profile.setBio(request.bio());
            }
            if (request.avatarUrl() != null) {
                profile.setAvatarUrl(request.avatarUrl());
            }
        }
        log.info("Обновлён пользователь id={}", id);
        return user;
    }


    @Transactional
    public void deleteUser(Long id) {
        User user = getUser(id);
        userRepository.delete(user);
        log.info("Удалён пользователь id={}", id);
    }
}
