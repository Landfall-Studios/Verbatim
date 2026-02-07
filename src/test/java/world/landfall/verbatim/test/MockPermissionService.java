package world.landfall.verbatim.test;

import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mock PermissionService for unit testing.
 * Allows setting up specific permissions per player.
 */
public class MockPermissionService extends PermissionService {
    private final Map<UUID, Set<String>> playerPermissions = new HashMap<>();
    private final Set<UUID> ops = new HashSet<>();

    public void grantPermission(GamePlayer player, String permission) {
        playerPermissions.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(permission);
    }

    public void revokePermission(GamePlayer player, String permission) {
        Set<String> perms = playerPermissions.get(player.getUUID());
        if (perms != null) {
            perms.remove(permission);
        }
    }

    public void setOp(GamePlayer player, boolean op) {
        if (op) {
            ops.add(player.getUUID());
        } else {
            ops.remove(player.getUUID());
        }
    }

    public void clear() {
        playerPermissions.clear();
        ops.clear();
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        return true; // Pretend we have a permission system for testing
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        // Check explicit permissions first
        Set<String> perms = playerPermissions.get(player.getUUID());
        if (perms != null && perms.contains(permissionNode)) {
            return true;
        }

        // Fall back to OP check
        return ops.contains(player.getUUID());
    }
}
