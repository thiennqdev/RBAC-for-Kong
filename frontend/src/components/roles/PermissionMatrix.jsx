import { useMemo } from 'react';

/**
 * PermissionMatrix — Resource × Action grid with checkboxes.
 * Dynamically builds columns based on all unique action names across all resources.
 *
 * Props:
 *   resources      - Array of ResourceResponse from backend
 *   permissions    - Array of PermissionResponse from backend
 *   selectedPerms  - Set<id> of currently selected permission IDs
 *   onToggle       - (id) => void — called when a single permission is toggled
 *   onBulkToggle   - (ids[], checked) => void — called for select-all operations
 *   readOnly       - boolean — if true, checkboxes are disabled
 */
export default function PermissionMatrix({
    resources = [],
    permissions = [],
    selectedPerms = new Set(),
    onToggle,
    onBulkToggle,
    readOnly = false,
}) {
    // Lookup permission ID by permissionName
    const permNameToId = useMemo(() => {
        const map = {};
        permissions.forEach(p => {
            map[p.permissionName] = p.id;
        });
        return map;
    }, [permissions]);

    // Gather all unique action columns present in the returned resources
    const actionColumns = useMemo(() => {
        const actionSet = new Set();
        // Preferred order for standard actions
        ['READ', 'WRITE', 'CREATE', 'DELETE'].forEach(a => actionSet.add(a));

        const usedActions = new Set();
        resources.forEach(res => {
            if (res.actions) {
                res.actions.forEach(a => {
                    actionSet.add(a.actionName);
                    usedActions.add(a.actionName);
                });
            }
        });

        // Return only columns that are actually used by at least one resource
        return Array.from(actionSet).filter(a => usedActions.has(a));
    }, [resources]);

    // All available permission IDs for "Select All"
    // We only consider permissions that are mapped to valid resources & actions
    const allMappablePermIds = useMemo(() => {
        const ids = [];
        resources.forEach(res => {
            if (res.actions) {
                res.actions.forEach(a => {
                    const id = permNameToId[a.permissionName];
                    if (id) ids.push(id);
                });
            }
        });
        return ids;
    }, [resources, permNameToId]);

    // Check if a specific resource row has all its available permissions selected
    function isRowAllSelected(resource) {
        if (!resource.actions || resource.actions.length === 0) return false;

        // Find valid permissions in this row
        const rowIds = resource.actions.map(a => permNameToId[a.permissionName]).filter(Boolean);
        if (rowIds.length === 0) return false;

        return rowIds.every((id) => selectedPerms.has(id));
    }

    // Check if ALL globally available permissions are selected
    function isAllSelected() {
        return allMappablePermIds.length > 0 && allMappablePermIds.every((id) => selectedPerms.has(id));
    }

    // Toggle all permissions in a resource row
    function toggleRow(resource) {
        if (!resource.actions) return;
        const rowIds = resource.actions.map(a => permNameToId[a.permissionName]).filter(Boolean);
        if (rowIds.length === 0) return;

        const allChecked = isRowAllSelected(resource);
        onBulkToggle?.(rowIds, !allChecked);
    }

    // Toggle all permissions globally
    function toggleAll() {
        const allChecked = isAllSelected();
        onBulkToggle?.(allMappablePermIds, !allChecked);
    }

    if (resources.length === 0 && permissions.length === 0) {
        return (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                No permissions available. Create resources and permissions first.
            </p>
        );
    }

    return (
        <div className="permission-matrix" style={{ overflowX: 'auto' }}>
            {/* Header */}
            <div className="permission-matrix-header">
                <div className="permission-matrix-resource">
                    <span className="resource-group-label">Administrator Access</span>
                </div>
                {actionColumns.map((action) => (
                    <div key={action} className="permission-matrix-action">
                        {action.charAt(0) + action.slice(1).toLowerCase()}
                    </div>
                ))}
                <div className="permission-matrix-action permission-matrix-select-all">
                    {!readOnly && (
                        <label className="matrix-checkbox-label">
                            <input
                                type="checkbox"
                                checked={isAllSelected()}
                                onChange={toggleAll}
                                className="matrix-checkbox"
                            />
                            <span>All</span>
                        </label>
                    )}
                </div>
            </div>

            {/* Rows */}
            {resources.map((resource) => {
                const rowAllChecked = isRowAllSelected(resource);

                return (
                    <div
                        key={resource.id}
                        className={`permission-matrix-row ${rowAllChecked ? 'row-all-checked' : ''}`}
                    >
                        <div className="permission-matrix-resource">
                            <span className="resource-name" title={resource.resourceKey}>
                                {resource.label || resource.resourceKey}
                            </span>
                            {resource.description && (
                                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                                    {resource.description}
                                </div>
                            )}
                        </div>
                        {actionColumns.map((colAction) => {
                            // Find if this resource has an action matching the column
                            const actionObj = resource.actions?.find(a => a.actionName === colAction);

                            if (!actionObj) {
                                return <div key={colAction} className="permission-matrix-action perm-empty">—</div>;
                            }

                            const permId = permNameToId[actionObj.permissionName];
                            if (!permId) {
                                // Action exists but no matching permission mapped (should not happen normally)
                                return <div key={colAction} className="permission-matrix-action perm-empty" title="Permission not synced">—</div>;
                            }

                            return (
                                <div key={colAction} className="permission-matrix-action">
                                    <label className="matrix-checkbox-label" title={actionObj.apiPath ? `${actionObj.httpMethod || '*'} ${actionObj.apiPath}` : actionObj.actionName}>
                                        <input
                                            type="checkbox"
                                            checked={selectedPerms.has(permId)}
                                            onChange={() => onToggle?.(permId)}
                                            disabled={readOnly}
                                            className="matrix-checkbox"
                                        />
                                        <span className="action-label" style={{ display: 'none' }}>
                                            {colAction}
                                        </span>
                                    </label>
                                </div>
                            );
                        })}
                        <div className="permission-matrix-action permission-matrix-select-all">
                            {!readOnly && (
                                <label className="matrix-checkbox-label">
                                    <input
                                        type="checkbox"
                                        checked={rowAllChecked}
                                        onChange={() => toggleRow(resource)}
                                        disabled={!resource.actions || resource.actions.length === 0}
                                        className="matrix-checkbox"
                                    />
                                </label>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
