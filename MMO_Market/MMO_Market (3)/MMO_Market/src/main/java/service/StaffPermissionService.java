package service;

import dal.PermissionRepository;
import dal.UserRepository;
import model.Permission;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StaffPermissionService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public StaffPermissionService(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<String> getStaffPermissions(Long staffId) {
        User staff = userRepository.findByIdWithPermissions(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên."));
        return staff.getUserPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignPermissions(List<Long> userIds, List<String> permissionNames) {
        List<Permission> permissions = permissionRepository.findByNameIn(permissionNames);
        if (permissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy quyền tương ứng.");
        }

        for (Long userId : userIds) {
            User user = userRepository.findByIdWithPermissions(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));
            
            if (user.getUserPermissions() == null) {
                user.setUserPermissions(new HashSet<>());
            }
            user.getUserPermissions().addAll(permissions);
            userRepository.save(user);
        }
    }

    @Transactional
    public void revokePermissions(Long userId, List<String> permissionNames) {
        User user = userRepository.findByIdWithPermissions(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + userId));

        if (user.getUserPermissions() != null) {
            user.getUserPermissions().removeIf(p -> permissionNames.contains(p.getName()));
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public List<User> searchStaffByPermissions(List<String> permissionNames, String groupName) {
        List<User> allStaffs = userRepository.findAllWithPermissionsByIsDeleteFalseOrderByCreatedAtDesc().stream()
                .filter(u -> u.getRole() != null && u.getRole().toLowerCase().contains("staff"))
                .collect(Collectors.toList());

        if (permissionNames == null || permissionNames.isEmpty()) {
            if (groupName == null || "ALL".equalsIgnoreCase(groupName)) {
                return allStaffs;
            } else {
                List<Permission> groupPerms = permissionRepository.findByGroupName(groupName);
                if (groupPerms.isEmpty()) return new ArrayList<>();
                List<String> groupPermNames = groupPerms.stream().map(Permission::getName).toList();
                
                return allStaffs.stream().filter(s -> {
                    Set<Permission> userPerms = s.getUserPermissions();
                    if (userPerms == null) return false;
                    List<String> userPermNames = userPerms.stream().map(Permission::getName).toList();
                    return userPermNames.containsAll(groupPermNames);
                }).collect(Collectors.toList());
            }
        }

        return allStaffs.stream().filter(s -> {
            Set<Permission> userPerms = s.getUserPermissions();
            if (userPerms == null) return false;
            List<String> userPermNames = userPerms.stream().map(Permission::getName).toList();
            return userPermNames.containsAll(permissionNames);
        }).collect(Collectors.toList());
    }
}
