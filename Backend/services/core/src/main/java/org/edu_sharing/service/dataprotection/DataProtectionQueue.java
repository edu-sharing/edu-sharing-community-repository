package org.edu_sharing.service.dataprotection;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataProtectionQueue {

    @Autowired
    DataProtectionConfig config;

    private final Object FILE_LOCK = new Object();

    String filePath;

    @PostConstruct
    public void init() throws IOException {
        filePath = config.getMainPath().concat("/").concat("tasks");
        File file = new File(filePath);
        if (!file.exists()) {
            boolean createOk = file.createNewFile();// creates parent folders as needed
            if (!createOk) {
                throw new RuntimeException("Unable to create file " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Add a user to the file
     * @param username
     * @return true when entry was added false when entry already exists
     */
    public boolean addUser(String username) {
        if(getAllUsers().contains(username)) return false;
        synchronized (FILE_LOCK) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                writer.write(username);
                writer.newLine();
            } catch (IOException e) {
                throw new RuntimeException("Error adding user",e);
            }
        }
        return true;
    }

    // Read all users from the file
    public List<String> getAllUsers() {
        synchronized (FILE_LOCK) {
            try {
                return Files.readAllLines(Paths.get(filePath))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Error reading users: " + e.getMessage());
                return Collections.emptyList();
            }
        }
    }

    // Remove users from the file
    public void removeUsers(List<String> usersToRemove) {
        synchronized (FILE_LOCK) {
            try {
                List<String> currentUsers = Files.readAllLines(Paths.get(filePath));
                currentUsers.removeAll(usersToRemove);
                Files.write(Paths.get(filePath), currentUsers, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.err.println("Error removing users: " + e.getMessage());
            }
        }
    }
}
