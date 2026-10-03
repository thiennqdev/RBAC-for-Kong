import { useState, useMemo, useEffect } from 'react';

export default function RolePermissionBuilder({
    resources = [],
    permissions = [],
    selectedPerms = new Set(),
    onToggle,
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

    // Track which resources have been explicitly "added" to the builder view
    const [addedResourceIds, setAddedResourceIds] = useState(new Set());
    // For the "Add Resource" dropdown
    const [selectedResourceToAdd, setSelectedResourceToAdd] = useState('');

    // Pre-populate added resources based on initial selected permissions
    useEffect(() => {
        if (resources.length === 0 || selectedPerms.size === 0) return;

        const initialAdded = new Set();
        resources.forEach(res => {
            if (!res.actions) return;
            const hasAnyPerm = res.actions.some(a => {
                const pId = permNameToId[a.permissionName];
                return pId && selectedPerms.has(pId);
            });
            if (hasAnyPerm) {
                initialAdded.add(res.id);
            }
        });

        setAddedResourceIds(prev => {
            const next = new Set(prev);
            let changed = false;
            initialAdded.forEach(id => {
                if (!next.has(id)) {
                    next.add(id);
                    changed = true;
                }
            });
            return changed ? next : prev;
        });
    }, [resources, selectedPerms, permNameToId]);

    const availableResourcesToAdd = useMemo(() => {
        return resources.filter(r => !addedResourceIds.has(r.id));
    }, [resources, addedResourceIds]);

    const addedResources = useMemo(() => {
        return resources.filter(r => addedResourceIds.has(r.id));
    }, [resources, addedResourceIds]);

    function handleAddResource() {
        if (!selectedResourceToAdd) return;
        setAddedResourceIds(prev => {
            const next = new Set(prev);
            next.add(Number(selectedResourceToAdd));
            return next;
        });
        setSelectedResourceToAdd('');
    }

    function handleRemoveResource(resourceId) {
        if (readOnly) return;
        // 1. Uncheck all permissions associated with this resource
        const resource = resources.find(r => r.id === resourceId);
        if (resource && resource.actions) {
            resource.actions.forEach(a => {
                const pId = permNameToId[a.permissionName];
                if (pId && selectedPerms.has(pId) && onToggle) {
                    onToggle(pId); // toggle off
                }
            });
        }

        // 2. Remove from the view
        setAddedResourceIds(prev => {
            const next = new Set(prev);
            next.delete(resourceId);
            return next;
        });
    }

    if (resources.length === 0 && permissions.length === 0 && !readOnly) {
        return (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                No permissions available. Create resources and permissions first.
            </p>
        );
    }

    if (readOnly && addedResources.length === 0) {
        return (
            <div className="empty-state" style={{ padding: '2rem 1rem' }}>
                <p style={{ color: 'var(--text-muted)' }}>No permissions assigned to this role.</p>
            </div>
        );
    }

    return (
        <div className="role-permission-builder">
            {!readOnly && (
                <div className="add-resource-bar" style={{ display: 'flex', gap: '8px', marginBottom: '24px', alignItems: 'center' }}>
                    <select
                        className="form-input"
                        style={{ maxWidth: '300px' }}
                        value={selectedResourceToAdd}
                        onChange={(e) => setSelectedResourceToAdd(e.target.value)}
                    >
                        <option value="">-- Add a Resource --</option>
                        {availableResourcesToAdd.map(r => (
                            <option key={r.id} value={r.id}>
                                {r.label || r.resourceKey}
                            </option>
                        ))}
                    </select>
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={handleAddResource}
                        disabled={!selectedResourceToAdd}
                    >
                        + Add
                    </button>
                    <div style={{ flex: 1 }}></div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        {addedResources.length} resource{addedResources.length !== 1 ? 's' : ''} configured
                    </div>
                </div>
            )}

            <div className="configured-resources" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {addedResources.length === 0 && !readOnly && (
                    <div className="empty-state" style={{ padding: '2rem 1rem', border: '1px dashed var(--border-color)', borderRadius: 'var(--radius-lg)' }}>
                        <p style={{ color: 'var(--text-muted)', margin: 0 }}>Select a resource from the dropdown above to start configuring permissions.</p>
                    </div>
                )}

                {addedResources.map(resource => (
                    <div key={resource.id} className="resource-card" style={{
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        padding: '16px',
                        backgroundColor: 'var(--surface-color)'
                    }}>
                        <div className="resource-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid var(--border-color)', paddingBottom: '12px', marginBottom: '16px' }}>
                            <div>
                                <h4 style={{ margin: '0 0 4px 0', fontSize: '1rem', color: 'var(--text-primary)' }}>
                                    {resource.label || resource.resourceKey}
                                </h4>
                                {resource.description && (
                                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-muted)' }}>{resource.description}</p>
                                )}
                            </div>
                            {!readOnly && (
                                <button
                                    type="button"
                                    onClick={() => handleRemoveResource(resource.id)}
                                    style={{ background: 'none', border: 'none', color: 'var(--danger-color)', cursor: 'pointer', fontSize: '0.85rem', padding: '4px 8px', borderRadius: '4px' }}
                                    title="Remove resource and all its permissions"
                                >
                                    ✕ Remove
                                </button>
                            )}
                        </div>

                        <div className="resource-actions-grid" style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
                            gap: '12px'
                        }}>
                            {(!resource.actions || resource.actions.length === 0) ? (
                                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>No actions defined for this resource.</p>
                            ) : (
                                resource.actions.map(actionObj => {
                                    const permId = permNameToId[actionObj.permissionName];
                                    if (!permId) return null;

                                    return (
                                        <label key={actionObj.id} className="matrix-checkbox-label" style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '8px',
                                            padding: '8px',
                                            borderRadius: '4px',
                                            backgroundColor: readOnly ? 'transparent' : 'var(--bg-color)',
                                            border: readOnly ? 'none' : '1px solid var(--border-color)',
                                            cursor: readOnly ? 'default' : 'pointer',
                                            opacity: readOnly && !selectedPerms.has(permId) ? 0.4 : 1
                                        }} title={actionObj.apiPath ? `${actionObj.httpMethod || '*'} ${actionObj.apiPath}` : ''}>
                                            <input
                                                type="checkbox"
                                                checked={selectedPerms.has(permId)}
                                                onChange={() => onToggle?.(permId)}
                                                disabled={readOnly}
                                                className="matrix-checkbox"
                                            />
                                            <span style={{ fontSize: '0.9rem', fontWeight: selectedPerms.has(permId) ? 600 : 400 }}>
                                                {actionObj.actionName}
                                            </span>
                                        </label>
                                    );
                                })
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
