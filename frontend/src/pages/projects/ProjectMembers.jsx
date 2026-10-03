import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import memberService from '../../services/memberService';
import userService from '../../services/userService';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function ProjectMembers() {
    const { projectCode } = useParams();
    const [members, setMembers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showAssign, setShowAssign] = useState(false);
    const [showEditRole, setShowEditRole] = useState(false);
    const [editingUser, setEditingUser] = useState(null);

    // Multi-select state
    const [searchKeyword, setSearchKeyword] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [searching, setSearching] = useState(false);
    const [showDropdown, setShowDropdown] = useState(false);

    const [roles, setRoles] = useState([]);
    const [selectedRoleId, setSelectedRoleId] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const searchRef = useRef(null);
    const debounceRef = useRef(null);
    const navigate = useNavigate();
    const toast = useToast();

    useEffect(() => { loadMembers(); }, [projectCode]);

    // Close dropdown on outside click
    useEffect(() => {
        function handleClickOutside(e) {
            if (searchRef.current && !searchRef.current.contains(e.target)) {
                setShowDropdown(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    async function loadMembers() {
        try {
            const res = await memberService.list(projectCode);
            setMembers(res.data || []);
        } catch (err) {
            toast.error('Failed to load members: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    async function openEditModal(user) {
        try {
            const loadedRoles = [
                { id: 'PROJECT_ADMIN', roleName: 'PROJECT_ADMIN' },
                { id: 'MAINTAINER', roleName: 'MAINTAINER' },
                { id: 'DEVELOPER', roleName: 'DEVELOPER' }
            ];
            setRoles(loadedRoles);
            setEditingUser(user);
            const currentRole = loadedRoles.find(r => r.roleName === user.roleName);
            setSelectedRoleId(currentRole ? currentRole.id : '');
            setShowEditRole(true);
        } catch (err) {
            toast.error('Failed to load roles: ' + err.message);
        }
    }

    async function handleEditRole(e) {
        e.preventDefault();
        if (!selectedRoleId) {
            toast.error('Please select a role');
            return;
        }
        setSubmitting(true);
        try {
            await memberService.updateRole(projectCode, editingUser.userId, { roleId: selectedRoleId });
            toast.success(`Role updated for ${editingUser.username}`);
            setShowEditRole(false);
            loadMembers();
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSubmitting(false);
        }
    }

    async function openAssignModal() {
        try {
            const loadedRoles = [
                { id: 'PROJECT_ADMIN', roleName: 'PROJECT_ADMIN' },
                { id: 'MAINTAINER', roleName: 'MAINTAINER' },
                { id: 'DEVELOPER', roleName: 'DEVELOPER' }
            ];
            setRoles(loadedRoles);
            setSelectedUsers([]);
            setSelectedRoleId('');
            setSearchKeyword('');
            setSearchResults([]);
            setShowAssign(true);
        } catch (err) {
            toast.error('Failed to load roles: ' + err.message);
        }
    }

    function handleSearchInput(value) {
        setSearchKeyword(value);
        setShowDropdown(true);

        // Debounce search
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(async () => {
            if (value.trim().length < 1) {
                setSearchResults([]);
                return;
            }
            setSearching(true);
            try {
                const res = await userService.search(value.trim());
                setSearchResults(res.data || []);
            } catch (err) {
                console.error('Search failed:', err);
            } finally {
                setSearching(false);
            }
        }, 300);
    }

    function toggleUser(user) {
        setSelectedUsers(prev => {
            const exists = prev.find(u => u.id === user.id);
            if (exists) return prev.filter(u => u.id !== user.id);
            return [...prev, user];
        });
    }

    function removeSelectedUser(userId) {
        setSelectedUsers(prev => prev.filter(u => u.id !== userId));
    }

    async function handleBatchAssign(e) {
        e.preventDefault();
        if (selectedUsers.length === 0) {
            toast.error('Please select at least one user');
            return;
        }
        if (!selectedRoleId) {
            toast.error('Please select a role');
            return;
        }
        setSubmitting(true);
        try {
            await memberService.batchAssign(projectCode, {
                userIds: selectedUsers.map(u => u.id),
                roleId: selectedRoleId
            });
            toast.success(`${selectedUsers.length} member(s) assigned!`);
            setShowAssign(false);
            loadMembers();
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleRemove(userId) {
        if (!confirm('Remove this member from the project?')) return;
        try {
            await memberService.remove(projectCode, userId);
            toast.success('Member removed');
            loadMembers();
        } catch (err) {
            toast.error(err.message);
        }
    }

    if (loading) return <LoadingSpinner />;

    // Filter out already-selected users from search results
    const filteredResults = searchResults.filter(
        u => !selectedUsers.find(s => s.id === u.id)
    );

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Members of <span style={{ color: 'var(--accent-primary)' }}>{projectCode}</span></h2>
                    <p>Manage user assignments and roles for this project</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}`)}>← Back</button>
                    <button className="btn btn-primary" onClick={openAssignModal}>+ Assign Members</button>
                </div>
            </div>

            <div className="data-table-wrapper">
                {members.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">👥</div>
                        <h3>No members assigned</h3>
                        <p>Click "Assign Members" to add users to this project</p>
                    </div>
                ) : (
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Assigned At</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {members.map((m) => (
                                <tr key={m.userId}>
                                    <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{m.username}</td>
                                    <td>{m.email}</td>
                                    <td><span className="permission-tag">{m.roleName}</span></td>
                                    <td>{m.assignedAt ? new Date(m.assignedAt).toLocaleDateString() : '-'}</td>
                                    <td>
                                        <button className="btn btn-sm btn-secondary" style={{ marginRight: '8px' }} onClick={() => openEditModal(m)}>Edit</button>
                                        <button className="btn btn-sm btn-danger" onClick={() => handleRemove(m.userId)}>Remove</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            <Modal isOpen={showAssign} onClose={() => setShowAssign(false)} title="Assign Members">
                <form onSubmit={handleBatchAssign}>
                    {/* User Search Multi-Select */}
                    <div className="form-group">
                        <label className="form-label">Search & Select Users *</label>
                        <div ref={searchRef} style={{ position: 'relative' }}>
                            <input
                                className="form-input"
                                value={searchKeyword}
                                onChange={(e) => handleSearchInput(e.target.value)}
                                onFocus={() => searchKeyword && setShowDropdown(true)}
                                placeholder="Type to search by username or email..."
                                autoComplete="off"
                            />

                            {/* Dropdown Results */}
                            {showDropdown && searchKeyword && (
                                <div style={{
                                    position: 'absolute',
                                    top: '100%',
                                    left: 0,
                                    right: 0,
                                    background: 'var(--bg-primary)',
                                    border: '1px solid var(--border-color)',
                                    borderRadius: '0 0 8px 8px',
                                    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                                    maxHeight: '200px',
                                    overflowY: 'auto',
                                    zIndex: 100,
                                }}>
                                    {searching ? (
                                        <div style={{ padding: '12px', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                                            Searching...
                                        </div>
                                    ) : filteredResults.length === 0 ? (
                                        <div style={{ padding: '12px', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                                            No users found
                                        </div>
                                    ) : (
                                        filteredResults.map(user => (
                                            <div
                                                key={user.id}
                                                onClick={() => toggleUser(user)}
                                                style={{
                                                    padding: '10px 14px',
                                                    cursor: 'pointer',
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'center',
                                                    borderBottom: '1px solid var(--border-color)',
                                                    transition: 'background 0.15s',
                                                }}
                                                onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-hover)'}
                                                onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                                            >
                                                <div>
                                                    <div style={{ fontWeight: 500, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                                                        {user.username}
                                                    </div>
                                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                                                        {user.email}
                                                    </div>
                                                </div>
                                                <span style={{ fontSize: '1.1rem', color: 'var(--accent-primary)' }}>+</span>
                                            </div>
                                        ))
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Selected Users Chips */}
                    {selectedUsers.length > 0 && (
                        <div style={{
                            display: 'flex',
                            flexWrap: 'wrap',
                            gap: '8px',
                            marginBottom: '16px',
                            padding: '12px',
                            background: 'var(--bg-secondary)',
                            borderRadius: '8px',
                            border: '1px solid var(--border-color)',
                        }}>
                            <div style={{
                                width: '100%',
                                fontSize: '0.75rem',
                                color: 'var(--text-secondary)',
                                textTransform: 'uppercase',
                                letterSpacing: '0.05em',
                                marginBottom: '4px',
                            }}>
                                Selected ({selectedUsers.length})
                            </div>
                            {selectedUsers.map(user => (
                                <span
                                    key={user.id}
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '6px',
                                        background: 'var(--accent-primary)',
                                        color: '#fff',
                                        padding: '4px 10px',
                                        borderRadius: '20px',
                                        fontSize: '0.82rem',
                                        fontWeight: 500,
                                    }}
                                >
                                    {user.username}
                                    <button
                                        type="button"
                                        onClick={() => removeSelectedUser(user.id)}
                                        style={{
                                            background: 'rgba(255,255,255,0.25)',
                                            border: 'none',
                                            color: '#fff',
                                            borderRadius: '50%',
                                            width: '18px',
                                            height: '18px',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            cursor: 'pointer',
                                            fontSize: '0.75rem',
                                            lineHeight: 1,
                                            padding: 0,
                                        }}
                                    >
                                        ×
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}

                    {/* Role Selection */}
                    <div className="form-group">
                        <label className="form-label">Select Role *</label>
                        <select
                            className="form-input"
                            value={selectedRoleId}
                            onChange={(e) => setSelectedRoleId(e.target.value)}
                            required
                        >
                            <option value="">-- Choose role --</option>
                            {roles.map((r) => (
                                <option key={r.id} value={r.id}>{r.roleName}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-actions">
                        <button type="button" className="btn btn-secondary" onClick={() => setShowAssign(false)}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={submitting || selectedUsers.length === 0}>
                            {submitting ? 'Assigning...' : `Assign ${selectedUsers.length} User(s)`}
                        </button>
                    </div>
                </form>
            </Modal>

            <Modal isOpen={showEditRole} onClose={() => setShowEditRole(false)} title="Edit Member Role">
                <form onSubmit={handleEditRole}>
                    <div className="form-group">
                        <label className="form-label">Member</label>
                        <input className="form-input" value={editingUser?.username || ''} disabled />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Select Role *</label>
                        <select
                            className="form-input"
                            value={selectedRoleId}
                            onChange={(e) => setSelectedRoleId(e.target.value)}
                            required
                        >
                            <option value="">-- Choose role --</option>
                            {roles.map((r) => (
                                <option key={r.id} value={r.id}>{r.roleName}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-actions">
                        <button type="button" className="btn btn-secondary" onClick={() => setShowEditRole(false)}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Saving...' : 'Save Role'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
