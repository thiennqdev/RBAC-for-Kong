import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import { useToast } from '../../components/common/Toast';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongUpstreamCreate() {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const { selectedKongInstance } = useContext(KongContext);
    
    const [formData, setFormData] = useState({
        name: '',
        algorithm: 'round-robin',
        slots: 10000,
        enableActiveHealthcheck: false,
        healthcheckPath: '/',
        healthcheckTimeout: 1,
        healthcheckInterval: 5,
        healthcheckSuccesses: 1,
        healthcheckHttpFailures: 2,
        healthcheckTcpFailures: 2,
        healthcheckTimeouts: 2,
        hashOn: 'none',
        hashFallback: 'none',
        hashOnHeader: '',
        hashFallbackHeader: '',
        hashOnCookie: '',
        hashOnCookiePath: '/',
        hashOnQueryArg: '',
        hashFallbackQueryArg: '',
        hashOnUriCapture: '',
        hashFallbackUriCapture: '',
        hostHeader: '',
        clientCertificate: '',
        useSrvName: false,
        tags: []
    });
    const [showAdvanced, setShowAdvanced] = useState(false);
    const [submitting, setSubmitting] = useState(false);

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
        
        const payload = {
            name: formData.name,
            algorithm: formData.algorithm,
            slots: formData.slots,
            hash_on: formData.hashOn,
            hash_fallback: formData.hashFallback,
            hash_on_header: formData.hashOnHeader || null,
            hash_fallback_header: formData.hashFallbackHeader || null,
            hash_on_cookie: formData.hashOnCookie || null,
            hash_on_cookie_path: formData.hashOnCookiePath || null,
            hash_on_query_arg: formData.hashOnQueryArg || null,
            hash_fallback_query_arg: formData.hashFallbackQueryArg || null,
            hash_on_uri_capture: formData.hashOnUriCapture || null,
            hash_fallback_uri_capture: formData.hashFallbackUriCapture || null,
            host_header: formData.hostHeader || null,
            client_certificate: formData.clientCertificate || null,
            use_srv_name: formData.useSrvName,
            tags: formData.tags.length > 0 ? formData.tags : null
        };
        
        if (formData.enableActiveHealthcheck) {
            payload.healthchecks = {
                active: {
                    type: "http",
                    http_path: formData.healthcheckPath,
                    timeout: formData.healthcheckTimeout,
                    healthy: {
                        interval: formData.healthcheckInterval,
                        successes: formData.healthcheckSuccesses
                    },
                    unhealthy: {
                        interval: formData.healthcheckInterval,
                        http_failures: formData.healthcheckHttpFailures,
                        tcp_failures: formData.healthcheckTcpFailures,
                        timeouts: formData.healthcheckTimeouts
                    }
                }
            };
        }
        
        try {
            await kongService.createUpstream(selectedKongInstance.id, projectCode, payload);
            toast.success('Upstream created successfully');
            navigate(`/projects/${projectCode}/kong/upstreams`);
        } catch (err) {
            toast.error(err.message || 'Failed to create upstream');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Create Kong Upstream</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Add a new upstream for load balancing in <strong>{projectCode}</strong></p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate(-1)}>← Cancel</button>
            </div>

            <div className="card animate-fade-in" style={{ maxWidth: '800px', borderTop: '4px solid #10b981' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
                    <div style={{ background: 'rgba(16,185,129,0.15)', color: '#10b981', padding: '8px', borderRadius: '8px' }}>⚖️</div>
                    <h3 style={{ margin: 0, fontSize: '1.2rem' }}>Upstream Details</h3>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Upstream Name <span style={{ color: 'red' }}>*</span></label>
                        <input 
                            type="text" 
                            name="name"
                            className="form-input" 
                            value={formData.name}
                            onChange={handleChange}
                            required
                            placeholder="e.g. user-service-cluster"
                        />
                        <small style={{ color: 'var(--text-muted)' }}>This can be used as a 'host' in a Service. Only lowercase letters, numbers, and hyphens.</small>
                    </div>

                    <div style={{ display: 'flex', gap: '16px' }}>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label className="form-label">Algorithm</label>
                            <select name="algorithm" className="form-input" value={formData.algorithm} onChange={handleChange}>
                                <option value="round-robin">Round Robin</option>
                                <option value="consistent-hashing">Consistent Hashing</option>
                                <option value="least-connections">Least Connections</option>
                                <option value="latency">Latency</option>
                            </select>
                            <small style={{ color: 'var(--text-muted)' }}>Load balancing algorithm.</small>
                        </div>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label className="form-label">Slots</label>
                            <input 
                                type="number" 
                                name="slots"
                                className="form-input" 
                                value={formData.slots}
                                onChange={handleChange}
                                required
                                min="10"
                                max="65536"
                            />
                            <small style={{ color: 'var(--text-muted)' }}>Number of slots in the load balancer (default 10000).</small>
                        </div>
                    </div>

                    <div style={{ marginTop: '32px', display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
                        <div style={{ background: 'rgba(59,130,246,0.15)', color: '#3b82f6', padding: '8px', borderRadius: '8px' }}>🏥</div>
                        <h3 style={{ margin: 0, fontSize: '1.2rem' }}>Active Healthchecks</h3>
                    </div>

                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <input 
                            type="checkbox" 
                            id="enableActiveHealthcheck"
                            name="enableActiveHealthcheck" 
                            checked={formData.enableActiveHealthcheck}
                            onChange={handleChange}
                            style={{ width: '18px', height: '18px' }}
                        />
                        <label htmlFor="enableActiveHealthcheck" style={{ margin: 0, fontWeight: 'bold' }}>Enable Active Healthchecks</label>
                    </div>

                    {formData.enableActiveHealthcheck && (
                        <div style={{ padding: '16px', background: 'var(--bg-secondary)', borderRadius: '8px', border: '1px solid var(--border-color)', marginBottom: '24px' }}>
                            <div className="form-group">
                                <label className="form-label">HTTP Path</label>
                                <input 
                                    type="text" 
                                    name="healthcheckPath"
                                    className="form-input" 
                                    value={formData.healthcheckPath}
                                    onChange={handleChange}
                                    required={formData.enableActiveHealthcheck}
                                    placeholder="/"
                                />
                                <small style={{ color: 'var(--text-muted)' }}>The path to use for health checks (e.g. /health).</small>
                            </div>
                            
                            <div style={{ display: 'flex', gap: '16px' }}>
                                <div className="form-group" style={{ flex: 1 }}>
                                    <label className="form-label">Timeout (seconds)</label>
                                    <input 
                                        type="number" 
                                        name="healthcheckTimeout"
                                        className="form-input" 
                                        value={formData.healthcheckTimeout}
                                        onChange={handleChange}
                                        required={formData.enableActiveHealthcheck}
                                        min="1"
                                    />
                                    <small style={{ color: 'var(--text-muted)' }}>Connection timeout.</small>
                                </div>
                                <div className="form-group" style={{ flex: 1 }}>
                                    <label className="form-label">Interval (seconds)</label>
                                    <input 
                                        type="number" 
                                        name="healthcheckInterval"
                                        className="form-input" 
                                        value={formData.healthcheckInterval}
                                        onChange={handleChange}
                                        required={formData.enableActiveHealthcheck}
                                        min="1"
                                    />
                                    <small style={{ color: 'var(--text-muted)' }}>Interval between health checks.</small>
                                </div>
                            </div>
                            
                            <div style={{ marginTop: '12px', borderTop: '1px dashed var(--border-color)', paddingTop: '16px' }}>
                                <h4 style={{ margin: '0 0 12px 0', fontSize: '0.95rem', color: 'var(--text-secondary)' }}>Thresholds</h4>
                                <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                                    <div className="form-group" style={{ flex: '1 1 calc(50% - 16px)' }}>
                                        <label className="form-label">Healthy Successes</label>
                                        <input 
                                            type="number" 
                                            name="healthcheckSuccesses"
                                            className="form-input" 
                                            value={formData.healthcheckSuccesses}
                                            onChange={handleChange}
                                            required={formData.enableActiveHealthcheck}
                                            min="1"
                                        />
                                        <small style={{ color: 'var(--text-muted)' }}>Successes to mark HEALTHY.</small>
                                    </div>
                                    <div className="form-group" style={{ flex: '1 1 calc(50% - 16px)' }}>
                                        <label className="form-label">HTTP Failures</label>
                                        <input 
                                            type="number" 
                                            name="healthcheckHttpFailures"
                                            className="form-input" 
                                            value={formData.healthcheckHttpFailures}
                                            onChange={handleChange}
                                            required={formData.enableActiveHealthcheck}
                                            min="0"
                                        />
                                        <small style={{ color: 'var(--text-muted)' }}>HTTP errors to mark UNHEALTHY.</small>
                                    </div>
                                    <div className="form-group" style={{ flex: '1 1 calc(50% - 16px)' }}>
                                        <label className="form-label">TCP Failures</label>
                                        <input 
                                            type="number" 
                                            name="healthcheckTcpFailures"
                                            className="form-input" 
                                            value={formData.healthcheckTcpFailures}
                                            onChange={handleChange}
                                            required={formData.enableActiveHealthcheck}
                                            min="0"
                                        />
                                        <small style={{ color: 'var(--text-muted)' }}>TCP errors to mark UNHEALTHY.</small>
                                    </div>
                                    <div className="form-group" style={{ flex: '1 1 calc(50% - 16px)' }}>
                                        <label className="form-label">Timeouts</label>
                                        <input 
                                            type="number" 
                                            name="healthcheckTimeouts"
                                            className="form-input" 
                                            value={formData.healthcheckTimeouts}
                                            onChange={handleChange}
                                            required={formData.enableActiveHealthcheck}
                                            min="0"
                                        />
                                        <small style={{ color: 'var(--text-muted)' }}>Timeouts to mark UNHEALTHY.</small>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                    
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
                                    <label className="form-label">Hash On</label>
                                    <select name="hashOn" className="form-input" value={formData.hashOn} onChange={handleChange}>
                                        <option value="none">None</option>
                                        <option value="consumer">Consumer</option>
                                        <option value="ip">IP</option>
                                        <option value="header">Header</option>
                                        <option value="cookie">Cookie</option>
                                        <option value="query_arg">Query Arg</option>
                                        <option value="uri_capture">URI Capture</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Hash Fallback</label>
                                    <select name="hashFallback" className="form-input" value={formData.hashFallback} onChange={handleChange}>
                                        <option value="none">None</option>
                                        <option value="consumer">Consumer</option>
                                        <option value="ip">IP</option>
                                        <option value="header">Header</option>
                                        <option value="cookie">Cookie</option>
                                        <option value="query_arg">Query Arg</option>
                                        <option value="uri_capture">URI Capture</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Hash On Header</label>
                                    <input type="text" name="hashOnHeader" className="form-input" value={formData.hashOnHeader} onChange={handleChange} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Hash Fallback Header</label>
                                    <input type="text" name="hashFallbackHeader" className="form-input" value={formData.hashFallbackHeader} onChange={handleChange} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Hash On Cookie</label>
                                    <input type="text" name="hashOnCookie" className="form-input" value={formData.hashOnCookie} onChange={handleChange} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Hash On Cookie Path</label>
                                    <input type="text" name="hashOnCookiePath" className="form-input" value={formData.hashOnCookiePath} onChange={handleChange} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Host Header</label>
                                    <input type="text" name="hostHeader" className="form-input" value={formData.hostHeader} onChange={handleChange} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Client Certificate ID</label>
                                    <input type="text" name="clientCertificate" className="form-input" value={formData.clientCertificate} onChange={handleChange} placeholder="UUID" />
                                </div>
                                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                                    <label className="form-label">Tags</label>
                                    <input type="text" className="form-input" value={formData.tags.join(', ')} onChange={handleTagsChange} placeholder="e.g. internal, payment, v1" />
                                </div>
                                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px', gridColumn: '1 / -1' }}>
                                    <input type="checkbox" name="useSrvName" id="useSrvName" checked={formData.useSrvName} onChange={handleChange} />
                                    <label htmlFor="useSrvName" style={{ margin: 0 }}>Use SRV Name</label>
                                </div>
                            </div>
                        )}
                    </div>

                    <div style={{ marginTop: '32px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
                        <button type="submit" className="btn btn-primary" style={{ background: '#10b981', borderColor: '#10b981' }} disabled={submitting}>
                            {submitting ? 'Creating...' : 'Create Upstream'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
