package cd.senat.medical.controller;

import cd.senat.medical.dto.RoleCreateRequest;
import cd.senat.medical.dto.RoleDTO;
import cd.senat.medical.dto.RoleRightsUpdateRequest;
import cd.senat.medical.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoleDTO> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleDTO created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String designation = body != null ? body.get("designation") : null;
        return ResponseEntity.ok(roleService.update(id, designation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            roleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("utilisateurs")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            if (msg != null && msg.contains("introuvable")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            throw e;
        }
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleDTO> updateRights(
            @PathVariable Long id,
            @RequestBody RoleRightsUpdateRequest request) {
        return ResponseEntity.ok(roleService.updateRights(id, request));
    }
}
