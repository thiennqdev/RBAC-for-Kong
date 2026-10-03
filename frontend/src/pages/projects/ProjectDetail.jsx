import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import projectService from '../../services/projectService';
import fileService from '../../services/fileService';
import StatusBadge from '../../components/common/StatusBadge';
import CopyToClipboard from '../../components/common/CopyToClipboard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';
import LiveSignalCards from '../../components/observability/LiveSignalCards';
import MetricsCharts from '../../components/observability/MetricsCharts';
import ProjectStorageModal from './ProjectStorageModal';
import memberService from '../../services/memberService';

function HardDeleteConfirm({ project, onConfirm, onCancel }) {
    const [typedCode, setTypedCode] = useState('');
    const confirmed = typedCode === project.projectCode;

    return (
        <div style={{
            position: 'fixed', inset: 0, zIndex: 1000,
            background: 'rgba(0,0,0,0.72)', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
        }}>
            <div style={{
                background: 'var(--bg-card)',
                border: '1px solid rgba(239,68,68,0.4)',
                borderRadius: '12px',
                padding: '28px 32px',
                width: '100%',
                maxWidth: '440px',
                boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
            }}>
                <div style={{ fontSize: '2rem', marginBottom: '10px' }}>⚠️</div>
                <h3 style={{ color: '#ef4444', marginBottom: '8px', fontSize: '1.1rem' }}>
                    Permanently Delete Project
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', marginBottom: '16px', lineHeight: 1.6 }}>
                    This will <strong style={{ color: '#ef4444' }}>permanently drop</strong> the database schema for{' '}
                    <code style={{ background: 'var(--bg-secondary)', padding: '2px 6px', borderRadius: '4px' }}>
                        {project.projectCode}
                    </code>{' '}
                    and remove the project from the system. <strong>All data will be lost forever.</strong>
                </p>
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
                        Type <strong style={{ color: 'var(--text-primary)' }}>{project.projectCode}</strong> to confirm:
                    </label>
                    <input
                        className="form-input"
                        autoFocus
                        value={typedCode}
                        onChange={(e) => setTypedCode(e.target.value)}
                        placeholder={project.projectCode}
                        style={{ borderColor: confirmed ? '#22c55e' : undefined }}
                    />
                </div>
                <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                    <button className="btn btn-secondary" onClick={onCancel}>Cancel</button>
                    <button
                        className="btn btn-danger"
                        disabled={!confirmed}
                        onClick={onConfirm}
                        style={{ opacity: confirmed ? 1 : 0.4, cursor: confirmed ? 'pointer' : 'not-allowed' }}
                    >
                        🗑️ Delete Permanently
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function ProjectDetail() {
    const { projectCode } = useParams();
    const [project, setProject] = useState(null);
    const [editing, setEditing] = useState(false);
    const [form, setForm] = useState({});
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [showHardDelete, setShowHardDelete] = useState(false);
    const [showSettings, setShowSettings] = useState(false);
    const [showStorageModal, setShowStorageModal] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();
    const [userRole, setUserRole] = useState(null);
    const { user } = useAuth();
    const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN');
    const isProjectAdmin = userRole === 'PROJECT_ADMIN';
    
    const fileInputRef = useRef(null);

    async function handleFileChange(e) {
        const file = e.target.files[0];
        if (!file) return;
        try {
            setSubmitting(true);
            const res = await fileService.uploadFile(file);
            setForm(prev => ({ ...prev, iconPath: res.filePath }));
            toast.success('File uploaded successfully');
        } catch (err) {
            toast.error(err.message || 'File upload failed');
        } finally {
            setSubmitting(false);
        }
    }

    useEffect(() => { loadProject(); }, [projectCode]);

    async function loadProject() {
        try {
            const res = await projectService.get(projectCode);
            setProject(res.data);
            setForm({
                projectName: res.data.projectName,
                iconPath: res.data.iconPath || '',
                status: res.data.status,
                idpType: res.data.idpType || 'DEFAULT',
                idpIssuerUrl: res.data.idpIssuerUrl || '',
                idpJwksUrl: res.data.idpJwksUrl || '',
                idpClientId: res.data.idpClientId || '',
                idpClientSecret: res.data.idpClientSecret || '',
                jwtAuthEnabled: res.data.jwtAuthEnabled !== false, // Default true if null
            });

            if (user) {
                try {
                    const membersRes = await memberService.list(projectCode);
                    const me = membersRes.data.find(m => m.username === user.username);
                    if (me) setUserRole(me.roleName);
                } catch (err) {
                    // Ignore error if user doesn't have permission to list members
                }
            }
        } catch (err) {
            toast.error('Failed to load project: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    async function handleUpdate(e) {
        e.preventDefault();
        setSubmitting(true);
        try {
            const res = await projectService.update(projectCode, form);
            setProject(res.data);
            setEditing(false);
            toast.success('Project updated!');
            window.dispatchEvent(new CustomEvent('project-updated'));
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleHardDelete() {
        try {
            await projectService.hardDelete(projectCode);
            toast.success(`Project "${projectCode}" permanently deleted`);
            navigate('/projects');
        } catch (err) {
            toast.error(err.message);
            setShowHardDelete(false);
        }
    }

    if (loading) return <LoadingSpinner />;
    if (!project) return <div className="empty-state"><h3>Project not found</h3></div>;

    return (
        <div>
            {showHardDelete && (
                <HardDeleteConfirm
                    project={project}
                    onConfirm={handleHardDelete}
                    onCancel={() => setShowHardDelete(false)}
                />
            )}

            {showStorageModal && (
                <ProjectStorageModal
                    projectCode={projectCode}
                    onClose={() => setShowStorageModal(false)}
                />
            )}

            {showSettings && (
                <div style={{
                    position: 'fixed', inset: 0, zIndex: 900,
                    background: 'rgba(0,0,0,0.6)', display: 'flex',
                    alignItems: 'center', justifyContent: 'center',
                    padding: '20px'
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '700px', maxHeight: '90vh', overflowY: 'auto', position: 'relative' }}>
                        <button 
                            onClick={() => { setShowSettings(false); setEditing(false); }}
                            style={{ position: 'absolute', top: 16, right: 16, background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.2rem' }}
                        >✕</button>
                        <h3 style={{ marginBottom: 20 }}>Project Information</h3>
                        
                        {editing ? (
                            <form onSubmit={handleUpdate}>
                                <div className="form-group">
                                    <label className="form-label">Project Name</label>
                                    <input className="form-input" value={form.projectName}
                                        onChange={(e) => setForm({ ...form, projectName: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Icon Path (Upload)</label>
                                    <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                                        <input className="form-input" value={form.iconPath}
                                            onChange={(e) => setForm({ ...form, iconPath: e.target.value })} placeholder="Image URL or upload" style={{ flex: 1 }} />
                                        <button type="button" className="btn btn-secondary" onClick={() => fileInputRef.current.click()}>Upload</button>
                                        <input type="file" ref={fileInputRef} style={{ display: 'none' }} accept="image/*" onChange={handleFileChange} />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Status</label>
                                    <select className="form-input" value={form.status}
                                        onChange={(e) => setForm({ ...form, status: e.target.value })}>
                                        <option value="ACTIVE">ACTIVE</option>
                                        <option value="INACTIVE">INACTIVE</option>
                                    </select>
                                </div>
                                {/* IdP Configuration Section */}
                                <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', marginTop: 16, paddingTop: 16 }}>
                                    <h4 style={{ marginBottom: 12, color: 'var(--text-secondary)', fontSize: '0.9rem' }}>🔐 Identity Provider Configuration</h4>

                                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px', padding: '12px', backgroundColor: 'rgba(34, 197, 94, 0.05)', border: '1px solid rgba(34, 197, 94, 0.2)', borderRadius: '6px' }}>
                                        <input type="checkbox" checked={form.jwtAuthEnabled} onChange={(e) => setForm({ ...form, jwtAuthEnabled: e.target.checked })} style={{ width: '18px', height: '18px', cursor: 'pointer' }} id="editJwtToggle" />
                                        <div>
                                            <label htmlFor="editJwtToggle" className="form-label" style={{ marginBottom: '4px', cursor: 'pointer', fontWeight: 600 }}>Enable Kong Gateway JWT Authentication</label>
                                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>If disabled, Kong Gateway will bypass JWT validation for this project's routes. This will sync to all existing Kong services!</div>
                                        </div>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">IdP Type</label>
                                        <select className="form-input" value={form.idpType}
                                            onChange={(e) => setForm({ ...form, idpType: e.target.value })}>
                                            <option value="DEFAULT">DEFAULT (System Keycloak Realm)</option>
                                            <option value="CUSTOM_KEYCLOAK">CUSTOM_KEYCLOAK</option>
                                            <option value="AUTH0">AUTH0</option>
                                            <option value="CUSTOM_OIDC">CUSTOM_OIDC</option>
                                        </select>
                                    </div>
                                    {form.idpType !== 'DEFAULT' && (
                                        <>
                                            <div className="form-group">
                                                <label className="form-label">Issuer URL</label>
                                                <input className="form-input" value={form.idpIssuerUrl}
                                                    onChange={(e) => setForm({ ...form, idpIssuerUrl: e.target.value })}
                                                    placeholder="https://auth.company.com/realms/my-realm" />
                                            </div>
                                            <div className="form-group">
                                                <label className="form-label">JWKS URL</label>
                                                <input className="form-input" value={form.idpJwksUrl}
                                                    onChange={(e) => setForm({ ...form, idpJwksUrl: e.target.value })}
                                                    placeholder="https://auth.company.com/realms/my-realm/protocol/openid-connect/certs" />
                                            </div>
                                            <div className="form-group">
                                                <label className="form-label">Client ID</label>
                                                <input className="form-input" value={form.idpClientId}
                                                    onChange={(e) => setForm({ ...form, idpClientId: e.target.value })}
                                                    placeholder="e.g. kong-client" />
                                            </div>
                                            <div className="form-group">
                                                <label className="form-label">Client Secret</label>
                                                <input type="password" className="form-input" value={form.idpClientSecret}
                                                    onChange={(e) => setForm({ ...form, idpClientSecret: e.target.value })}
                                                    placeholder="Client Secret" />
                                            </div>
                                        </>
                                    )}
                                </div>
                                <div className="form-actions">
                                    <button type="button" className="btn btn-secondary" onClick={() => setEditing(false)}>Cancel</button>
                                    <button type="submit" className="btn btn-primary" disabled={submitting}>
                                        {submitting ? 'Saving...' : 'Save'}
                                    </button>
                                </div>
                            </form>
                        ) : (
                            <div className="detail-grid">
                                <div className="detail-item">
                                    <div className="detail-label">Project Code</div>
                                    <div className="detail-value" style={{ fontFamily: 'monospace', color: 'var(--accent-primary)' }}>{project.projectCode}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Project Name</div>
                                    <div className="detail-value">{project.projectName}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Status</div>
                                    <div className="detail-value"><StatusBadge status={project.status} /></div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Icon</div>
                                    <div className="detail-value">
                                        {project.iconPath ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                <img src={fileService.getFileUrl(project.iconPath)} alt="Icon" style={{ width: 32, height: 32, objectFit: 'cover', borderRadius: 4 }} />
                                                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>{project.iconPath}</span>
                                            </div>
                                        ) : '-'}
                                    </div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Created At</div>
                                    <div className="detail-value">{project.createdAt ? new Date(project.createdAt).toLocaleString() : '-'}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Created By</div>
                                    <div className="detail-value">{project.createdBy || '-'}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">Last Updated</div>
                                    <div className="detail-value">{project.updatedAt ? new Date(project.updatedAt).toLocaleString() : '-'}</div>
                                </div>
                                {/* IdP Configuration Display */}
                                <div className="detail-item" style={{ gridColumn: '1 / -1', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 12, marginTop: 8 }}>
                                    <div className="detail-label">🔐 Identity Provider</div>
                                    <div className="detail-value" style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                                        <span style={{
                                            padding: '3px 10px', borderRadius: 6, fontSize: '0.82rem', fontWeight: 600,
                                            background: project.idpType === 'DEFAULT' ? 'rgba(34,197,94,0.12)' : 'rgba(168,85,247,0.12)',
                                            color: project.idpType === 'DEFAULT' ? '#22c55e' : '#a855f7',
                                        }}>
                                            {project.idpType || 'DEFAULT'}
                                        </span>
                                        <span style={{
                                            padding: '3px 10px', borderRadius: 6, fontSize: '0.82rem', fontWeight: 600,
                                            background: project.jwtAuthEnabled !== false ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
                                            color: project.jwtAuthEnabled !== false ? '#22c55e' : '#ef4444',
                                        }}>
                                            {project.jwtAuthEnabled !== false ? '✅ Kong JWT Enabled' : '❌ Kong JWT Disabled'}
                                        </span>
                                    </div>
                                </div>
                                {project.idpType && project.idpType !== 'DEFAULT' && (
                                    <>
                                        <div className="detail-item">
                                            <div className="detail-label">Issuer URL</div>
                                            <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.82rem', wordBreak: 'break-all' }}>{project.idpIssuerUrl || '-'}</div>
                                        </div>
                                        <div className="detail-item">
                                            <div className="detail-label">JWKS URL</div>
                                            <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.82rem', wordBreak: 'break-all' }}>{project.idpJwksUrl || '-'}</div>
                                        </div>
                                    </>
                                )}
                                <div className="detail-item" style={{ gridColumn: '1 / -1' }}>
                                    <div className="detail-label">Client ID {(!project.idpType || project.idpType === 'DEFAULT') ? '(Auto-Generated)' : ''}</div>
                                    <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.82rem', display: 'flex', alignItems: 'center' }}>
                                        {project.idpClientId || '-'}
                                        {project.idpClientId && <CopyToClipboard text={project.idpClientId} />}
                                    </div>
                                    {(!project.idpType || project.idpType === 'DEFAULT') && project.idpClientId && (
                                        <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: 4 }}>
                                            This Client ID was automatically generated in the shared realm. Use it to authenticate your frontend/clients to be properly isolated by Kong Gateway.
                                        </div>
                                    )}
                                </div>
                                <div className="detail-item" style={{ gridColumn: '1 / -1' }}>
                                    <div className="detail-label">Client Secret</div>
                                    <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.82rem', display: 'flex', alignItems: 'center' }}>
                                        {project.idpClientSecret ? (
                                            <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                <input 
                                                    type="password" 
                                                    value={project.idpClientSecret} 
                                                    readOnly 
                                                    style={{ 
                                                        background: 'transparent', 
                                                        border: 'none', 
                                                        color: 'inherit', 
                                                        fontFamily: 'inherit',
                                                        outline: 'none',
                                                        width: '100px'
                                                    }} 
                                                />
                                                <CopyToClipboard text={project.idpClientSecret} />
                                            </span>
                                        ) : '-'}
                                    </div>
                                </div>

                                <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '8px', marginTop: 16 }}>
                                    <button className="btn btn-secondary" onClick={() => setEditing(true)}>✏️ Edit Project</button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <div className="page-header">
                <div>
                    <h2>Overview: {project.projectName}</h2>
                    <p style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)' }}>
                        Tenant: <span style={{ fontFamily: 'monospace', color: 'var(--accent-primary)' }}>{project.projectCode}</span>
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    <button className="btn btn-secondary" onClick={() => navigate('/projects')}>← Back</button>
                    {isSystemAdmin && (
                        <button className="btn btn-primary" onClick={() => navigate(`/projects/${projectCode}/members`)}>
                            👥 Members
                        </button>
                    )}
                    <button className="btn btn-secondary" onClick={() => setShowSettings(true)}>⚙️ Project Info</button>
                    {(isSystemAdmin || isProjectAdmin) && (
                        <button className="btn btn-secondary" onClick={() => setShowStorageModal(true)}>💾 Storage</button>
                    )}
                    {isSystemAdmin && (
                        <button
                            className="btn btn-danger"
                            onClick={() => setShowHardDelete(true)}
                            title="Permanently delete project and all its data"
                        >
                            🗑️ Delete
                        </button>
                    )}
                </div>
            </div>

            {!isSystemAdmin ? (
                <>
                    <LiveSignalCards projectCode={projectCode} />
                    <MetricsCharts projectCode={projectCode} />
                </>
            ) : (
                <div className="empty-state">
                    <div className="empty-icon">🛡️</div>
                    <h3>System Administrator View</h3>
                    <p>System administrators manage project configuration and access.</p>
                    <p>Tenant metrics (Real-time traffic) are only available to project members.</p>
                    <button className="btn btn-primary" onClick={() => setShowSettings(true)} style={{ marginTop: 16 }}>⚙️ View Project Settings</button>
                </div>
            )}
        </div>
    );
}
