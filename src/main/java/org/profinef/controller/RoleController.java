package org.profinef.controller;

import org.profinef.entity.ERole;
import org.profinef.entity.Role;
import org.profinef.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    @Autowired
    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    @GetMapping
    public List<Role> getAll() {
        return (List<Role>) roleRepository.findAll();
    }
    public Role get(@RequestBody ERole role) {
        return roleRepository.findByName(role).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
    }
}
