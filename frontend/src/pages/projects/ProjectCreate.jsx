import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import projectService from '../../services/projectService';
import fileService from '../../services/fileService';
import { useToast } from '../../components/common/Toast';

export default function ProjectCreate() {
    const [form, setForm] = useState({ 
        projectCode: '', 
        projectName: '', 
        iconPath: '',
        idpType: 'DEFAULT',
        idpIssuerUrl: '',
        idpJwksUrl: '',
        idpClientId: '',
        idpClientSecret: '',
        jwtAuthEnabled: true
    });
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();

    const fileInputRef = useRef(null);

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        const val = type === 'checkbox' ? checked : value;
        if (name === 'projectName') {
            const generatedCode = val.toLowerCase()
                .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-+|-+$/g, '');
            setForm(prev => ({ ...prev, projectName: val, projectCode: generatedCode }));
        } else {
            setForm(prev => ({ ...prev, [name]: val }));
        }
    }

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

    async function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        try {
            await projectService.create(form);
            toast.success('Project created successfully!');
            navigate('/projects');
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Create Project</h2>
                    <p>Set up a new tenant project with isolated database schema</p>
                </div>
            </div>

            <div className="card" style={{ maxWidth: '800px' }}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Project Name *</label>
                        <input className="form-input" name="projectName" value={form.projectName} onChange={handleChange} required placeholder="e.g. Demo Project 2024" />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Project Code *</label>
                        <input className="form-input" name="projectCode" value={form.projectCode} onChange={handleChange}
                            required pattern="^[a-zA-Z0-9-]{3,50}$" placeholder="e.g. demo-project-2024" />
                        <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Letters, numbers, and hyphens only (3-50 chars)</small>
                    </div>
                    <div className="form-group">
                        <label className="form-label">Thumbnail (MinIO Upload)</label>
                        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                            <input className="form-input" name="iconPath" value={form.iconPath} onChange={handleChange} placeholder="Image URL or upload" style={{ flex: 1 }} />
                            <button type="button" className="btn btn-secondary" onClick={() => fileInputRef.current.click()}>Upload</button>
                            <input type="file" ref={fileInputRef} style={{ display: 'none' }} accept="image/*" onChange={handleFileChange} />
                        </div>
                    </div>

                    <hr style={{ margin: '20px 0', border: '1px solid var(--border-color)' }} />
                    <h3 style={{ marginBottom: '15px' }}>Identity Provider (IdP) Configuration</h3>

                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px', padding: '12px', backgroundColor: 'rgba(34, 197, 94, 0.05)', border: '1px solid rgba(34, 197, 94, 0.2)', borderRadius: '6px' }}>
                        <input type="checkbox" name="jwtAuthEnabled" checked={form.jwtAuthEnabled} onChange={handleChange} style={{ width: '18px', height: '18px', cursor: 'pointer' }} id="jwtToggle" />
                        <div>
                            <label htmlFor="jwtToggle" className="form-label" style={{ marginBottom: '4px', cursor: 'pointer', fontWeight: 600 }}>Enable Kong Gateway JWT Authentication</label>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>If disabled, Kong Gateway will bypass JWT validation for this project's routes. Useful if you already have Keycloak integrated separately.</div>
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">IdP Type</label>
                        <select className="form-input" name="idpType" value={form.idpType} onChange={handleChange}>
                            <option value="DEFAULT">Default (Shared Keycloak)</option>
                            <option value="CUSTOM_OIDC">Custom OIDC</option>
                            <option value="CUSTOM_KEYCLOAK">Custom Keycloak</option>
                            <option value="AUTH0">Auth0</option>
                        </select>
                    </div>

                    {form.idpType !== 'DEFAULT' && (
                        <div style={{ padding: '15px', backgroundColor: 'var(--bg-card)', border: '1px dashed var(--border-color)', borderRadius: '4px' }}>
                            <div className="form-group">
                                <label className="form-label">Issuer URL</label>
                                <input className="form-input" name="idpIssuerUrl" value={form.idpIssuerUrl} onChange={handleChange} placeholder="e.g. https://auth.company.com/realms/my-realm" />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Client ID</label>
                                <input className="form-input" name="idpClientId" value={form.idpClientId} onChange={handleChange} placeholder="e.g. kong-client" />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Client Secret</label>
                                <input type="password" className="form-input" name="idpClientSecret" value={form.idpClientSecret} onChange={handleChange} placeholder="Client Secret" />
                            </div>
                            <div className="form-group" style={{ marginBottom: 0 }}>
                                <label className="form-label">JWKS URL (Optional)</label>
                                <input className="form-input" name="idpJwksUrl" value={form.idpJwksUrl} onChange={handleChange} placeholder="e.g. https://auth.company.com/realms/my-realm/protocol/openid-connect/certs" />
                            </div>
                        </div>
                    )}

                    <div className="form-actions" style={{ marginTop: '20px' }}>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate('/projects')}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Creating...' : 'Create Project'}</button>
                    </div>
                </form>
            </div>
        </div>
    );
}
