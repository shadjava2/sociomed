package cd.senat.medical.controller;

import cd.senat.medical.dto.PermissionDTO;
import cd.senat.medical.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<PermissionDTO>> findAll() {
        return ResponseEntity.<List<PermissionDTO>>ok(permissionService.findAll());
    }
}
