import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import { useToast } from '../../components/common/Toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongServiceEdit() {
    const { projectCode, serviceId } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const { selectedKongInstance } = useContext(KongContext);
    
    const [loading, setLoading] = useState(true);
    const [formData, setFormData] = useState({
        name: '',
        host: '',
        port: 80,
        protocol: 'http',
        path: '',
        enabled: true,
        tags: [],
        retries: 5,
        connectTimeout: 60000,
        writeTimeout: 60000,
        readTimeout: 60000,
        clientCertificate: '',
        tlsVerify: false,
        tlsVerifyDepth: null
    });
    const [showAdvanced, setShowAdvanced] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (selectedKongInstance) loadService();
    }, [projectCode, serviceId, selectedKongInstance]);

    const loadService = async () => {
        try {
            const res = await kongService.getService(selectedKongInstance.id, projectCode, serviceId);
            const data = res.data;
            setFormData({
                name: data.displayName || data.name,
                host: data.host || '',
                port: data.port || 80,
                protocol: data.protocol || 'http',
                path: data.path || '',
                enabled: data.enabled !== false,
                tags: data.tags || [],
                retries: data.retries ?? 5,
                connectTimeout: data.connectTimeout ?? 60000,
                writeTimeout: data.writeTimeout ?? 60000,
                readTimeout: data.readTimeout ?? 60000,
                clientCertificate: data.clientCertificate || '',
                tlsVerify: data.tlsVerify ?? false,
                tlsVerifyDepth: data.tlsVerifyDepth ?? null
            });
        } catch (err) {
            toast.error(err.message || 'Failed to load service');
            navigate(`/projects/${projectCode}/kong/services`);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : (type === 'number' ? Number(value) : value)
        }));
    };

    const handleTagsChange = (e) => {
        const value = e.target.value;
        const tagsArray = value.split(',').map(t => t.trim()).filter(t => t !== '');
        setFormData(prev => ({ ...prev, tags: tagsArray }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await kongService.updateService(selectedKongInstance.id, projectCode, serviceId, formData);
            toast.success('Kong Service updated successfully');
            navigate(`/projects/${projectCode}/kong/services/${serviceId}`);
        } catch (err) {
            toast.error(err.message || 'Failed to update service');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Edit Service</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Update upstream backend target</p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate(-1)}>← Cancel</button>
            </div>

            <div className="card animate-fade-in" style={{ maxWidth: '800px' }}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Service Name <span style={{ color: 'red' }}>*</span></label>
                        <input 
                            type="text" 
                            name="name"
                            className="form-input" 
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '16px' }}>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label className="form-label">Protocol</label>
                            <select name="protocol" className="form-input" value={formData.protocol} onChange={handleChange}>
                                <option value="http">HTTP</option>
                                <option value="https">HTTPS</option>
                                <option value="grpc">GRPC</option>
                                <option value="grpcs">GRPCS</option>
                                <option value="tcp">TCP</option>
                                <option value="udp">UDP</option>
                                <option value="tls">TLS</option>
                            </select>
                        </div>
                        <div className="form-group" style={{ flex: 2 }}>
                            <label className="form-label">Host <span style={{ color: 'red' }}>*</span></label>
                            <input 
                                type="text" 
                                name="host"
                                className="form-input" 
                                value={formData.host}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label className="form-label">Port <span style={{ color: 'red' }}>*</span></label>
                            <input 
                                type="number" 
                                name="port"
                                className="form-input" 
                                value={formData.port}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Path</label>
                        <input 
                            type="text" 
                            name="path"
                            className="form-input" 
                            value={formData.path}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Tags</label>
                        <input 
                            type="text" 
                            className="form-input" 
                            value={formData.tags.join(', ')}
                            onChange={handleTagsChange}
                        />
                    </div>

                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '24px' }}>
                        <input 
                            type="checkbox" 
                            name="enabled"
                            id="enabled"
                            checked={formData.enabled}
                            onChange={handleChange}
                            style={{ width: '18px', height: '18px' }}
                        />
                        <label htmlFor="enabled" style={{ margin: 0, fontWeight: 500 }}>Enable Service</label>
                    </div>

                    <div style={{ marginTop: '24px', borderTop: '1px solid var(--border-color)', paddingTop: '16px' }}>
                        <button 
                            type="button" 
                            className="btn btn-secondary" 
                            onClick={() => setShowAdvanced(!showAdvanced)}
                            style={{ marginBottom: '16px' }}
                        >
                            {showAdvanced ? 'Hide Advanced Settings' : 'Show Advanced Settings'}
                        </button>

                        {showAdvanced && (
                            <div className="animate-fade-in" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', background: 'var(--bg-secondary)', padding: '16px', borderRadius: '8px' }}>
                                <div className="form-group">
                                    <label className="form-label">Retries</label>
                                    <input type="number" name="retries" className="form-input" value={formData.retries} onChange={handleChange} min="0" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Connect Timeout (ms)</label>
                                    <input type="number" name="connectTimeout" className="form-input" value={formData.connectTimeout} onChange={handleChange} min="1" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Write Timeout (ms)</label>
                                    <input type="number" name="writeTimeout" className="form-input" value={formData.writeTimeout} onChange={handleChange} min="1" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Read Timeout (ms)</label>
                                    <input type="number" name="readTimeout" className="form-input" value={formData.readTimeout} onChange={handleChange} min="1" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Client Certificate ID</label>
                                    <input type="text" name="clientCertificate" className="form-input" value={formData.clientCertificate} onChange={handleChange} placeholder="UUID" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">TLS Verify Depth</label>
                                    <input type="number" name="tlsVerifyDepth" className="form-input" value={formData.tlsVerifyDepth || ''} onChange={handleChange} min="0" placeholder="Default" />
                                </div>
                                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px', gridColumn: '1 / -1' }}>
                                    <input type="checkbox" name="tlsVerify" id="tlsVerify" checked={formData.tlsVerify} onChange={handleChange} />
                                    <label htmlFor="tlsVerify" style={{ margin: 0 }}>Enable TLS Verify</label>
                                </div>
                            </div>
                        )}
                    </div>

                    <div style={{ marginTop: '32px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
