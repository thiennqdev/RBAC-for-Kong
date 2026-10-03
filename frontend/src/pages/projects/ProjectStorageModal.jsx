import { useState, useEffect } from 'react';
import storageService from '../../services/storageService';
import CopyToClipboard from '../../components/common/CopyToClipboard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function ProjectStorageModal({ projectCode, onClose }) {
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [config, setConfig] = useState(null);
    const [form, setForm] = useState({ quota: 1, quotaUnit: 'GB', isPublic: false }); // Default 1GB
    const toast = useToast();

    useEffect(() => {
        loadStorageConfig();
    }, [projectCode]);

    async function loadStorageConfig() {
        try {
            const res = await storageService.getStorageConfig(projectCode);
            setConfig(res.data);
        } catch (err) {
            // 404 or 400 if not enabled
            setConfig(null);
        } finally {
            setLoading(false);
        }
    }

    async function handleEnableStorage(e) {
        e.preventDefault();
        setSubmitting(true);
        try {
            const res = await storageService.enableStorage(projectCode, form);
            setConfig(res.data);
            toast.success('Storage enabled successfully');
        } catch (err) {
            toast.error(err.message || 'Failed to enable storage');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleRotateCredentials() {
        if (!window.confirm("Are you sure you want to revoke the current secret key and generate a new one? Applications using the old key will immediately lose access.")) {
            return;
        }
        setSubmitting(true);
        try {
            const res = await storageService.rotateCredentials(projectCode);
            setConfig(res.data);
            toast.success('Credentials rotated successfully');
        } catch (err) {
            toast.error(err.message || 'Failed to rotate credentials');
        } finally {
            setSubmitting(false);
        }
    }

    const formatBytes = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    return (
        <div style={{
            position: 'fixed', inset: 0, zIndex: 900,
            background: 'rgba(0,0,0,0.6)', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
            padding: '20px'
        }}>
            <div className="card" style={{ width: '100%', maxWidth: '700px', maxHeight: '90vh', overflowY: 'auto', position: 'relative', display: 'flex', flexDirection: 'column' }}>
                <button 
                    onClick={onClose}
                    style={{ position: 'absolute', top: 16, right: 16, background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.2rem' }}
                >✕</button>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: 20 }}>
                    <div style={{ fontSize: '1.8rem' }}>💾</div>
                    <div>
                        <h3 style={{ margin: 0 }}>File Management (Object Storage)</h3>
                        <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Manage storage bucket for {projectCode}</p>
                    </div>
                </div>

                {loading ? <LoadingSpinner /> : (
                    config ? (
                        <>
                            <div className="detail-grid">
                                    <div className="detail-item">
                                        <div className="detail-label">Status</div>
                                        <div className="detail-value">
                                            <span style={{ color: '#22c55e', background: 'rgba(34,197,94,0.1)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>
                                                {config.status}
                                            </span>
                                        </div>
                                    </div>
                                    <div className="detail-item">
                                        <div className="detail-label">Bucket Name</div>
                                        <div className="detail-value" style={{ fontFamily: 'monospace', color: 'var(--accent-primary)' }}>
                                            {config.bucketName}
                                        </div>
                                    </div>
                                    <div className="detail-item">
                                        <div className="detail-label">Quota</div>
                                        <div className="detail-value">
                                            {formatBytes(config.quotaInBytes)}
                                        </div>
                                    </div>
                                    
                                    <div className="detail-item" style={{ gridColumn: '1 / -1', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 12, marginTop: 8 }}>
                                        <div className="detail-label">🔑 Access Credentials</div>
                                        <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '10px' }}>
                                            Use these credentials to connect to the MinIO/S3 compatible API. 
                                            These keys have exclusive access to the <strong>{config.bucketName}</strong> bucket.
                                        </p>
                                    </div>
                                    
                                    <div className="detail-item" style={{ gridColumn: '1 / -1' }}>
                                        <div className="detail-label">Access Key</div>
                                        <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.85rem', display: 'flex', alignItems: 'center' }}>
                                            {config.accessKey}
                                            <CopyToClipboard text={config.accessKey} />
                                        </div>
                                    </div>
                                    <div className="detail-item" style={{ gridColumn: '1 / -1' }}>
                                        <div className="detail-label">Secret Key</div>
                                        <div className="detail-value" style={{ fontFamily: 'monospace', fontSize: '0.85rem', display: 'flex', alignItems: 'center' }}>
                                            {config.secretKey ? (
                                                <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                    <span style={{ color: '#ef4444', fontWeight: 'bold' }}>{config.secretKey}</span>
                                                    <CopyToClipboard text={config.secretKey} />
                                                    <span style={{ fontSize: '0.75rem', color: '#fbbf24', marginLeft: '10px' }}>⚠️ Copy this now. You won't be able to see it again!</span>
                                                </span>
                                            ) : (
                                                <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                    <span style={{ letterSpacing: '2px', color: 'var(--text-muted)' }}>****************</span>
                                                    <button 
                                                        onClick={handleRotateCredentials} 
                                                        className="btn btn-secondary" 
                                                        style={{ padding: '4px 12px', fontSize: '0.75rem', marginLeft: '10px', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', border: '1px solid rgba(239, 68, 68, 0.3)' }}
                                                        disabled={submitting}
                                                    >
                                                        🔄 Revoke & Regenerate
                                                    </button>
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                        </>
                    ) : (
                        <div>
                            <div className="empty-state" style={{ marginBottom: 20, padding: '20px' }}>
                                <h4 style={{ margin: '0 0 10px 0' }}>Storage Not Enabled</h4>
                                <p style={{ margin: 0, fontSize: '0.9rem' }}>File management is not configured for this project. Enable it to provision an isolated S3 bucket.</p>
                            </div>
                            <form onSubmit={handleEnableStorage}>
                                <div className="form-group">
                                    <label className="form-label">Quota Capacity</label>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <input 
                                            type="number" 
                                            className="form-input" 
                                            style={{ flex: 1 }}
                                            value={form.quota}
                                            onChange={(e) => setForm({ ...form, quota: parseInt(e.target.value) || 0 })} 
                                            min="1"
                                        />
                                        <select 
                                            className="form-input" 
                                            style={{ width: '100px' }}
                                            value={form.quotaUnit}
                                            onChange={(e) => setForm({ ...form, quotaUnit: e.target.value })}
                                        >
                                            <option value="MB">MB</option>
                                            <option value="GB">GB</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <input 
                                        type="checkbox" 
                                        id="isPublic"
                                        checked={form.isPublic}
                                        onChange={(e) => setForm({ ...form, isPublic: e.target.checked })} 
                                    />
                                    <label htmlFor="isPublic" style={{ margin: 0, fontSize: '0.9rem' }}>Make bucket public (objects can be downloaded without authentication)</label>
                                </div>
                                <div className="form-actions" style={{ marginTop: 20 }}>
                                    <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
                                    <button type="submit" className="btn btn-primary" disabled={submitting}>
                                        {submitting ? 'Provisioning...' : 'Enable Storage'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    )
                )}
            </div>
        </div>
    );
}
