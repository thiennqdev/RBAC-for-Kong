import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import kongInstanceService from '../../services/kongInstanceService';
import { useToast } from '../../components/common/Toast';

export default function KongInstanceCreate() {
    const navigate = useNavigate();
    const toast = useToast();
    const [submitting, setSubmitting] = useState(false);
    const [formData, setFormData] = useState({
        code: '',
        name: '',
        adminUrl: '',
        description: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await kongInstanceService.createInstance(formData);
            toast.success('Kong Instance registered successfully!');
            navigate('/kong-instances');
        } catch (err) {
            toast.error(err.response?.data?.message || err.message || 'Failed to register Kong Instance');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Register Kong Instance</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Add a new API Gateway environment</p>
                </div>
            </div>

            <div className="card" style={{ maxWidth: '600px' }}>
                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                    <div className="form-group">
                        <label>Environment Code *</label>
                        <input 
                            className="form-input" 
                            name="code"
                            value={formData.code}
                            onChange={handleChange}
                            placeholder="e.g., DEV, PROD, STAGING" 
                            required 
                            pattern="^[a-zA-Z0-9_-]+$"
                            title="Only alphanumeric, dashes, and underscores allowed"
                        />
                        <small style={{ color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>A unique short code identifying this environment.</small>
                    </div>

                    <div className="form-group">
                        <label>Display Name *</label>
                        <input 
                            className="form-input" 
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="e.g., Development Cluster" 
                            required 
                        />
                    </div>

                    <div className="form-group">
                        <label>Admin API URL *</label>
                        <input 
                            className="form-input" 
                            type="url"
                            name="adminUrl"
                            value={formData.adminUrl}
                            onChange={handleChange}
                            placeholder="http://kong-admin:8001" 
                            required 
                        />
                        <small style={{ color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>The internal URL for the Kong Admin API (port 8001).</small>
                    </div>

                    <div className="form-group">
                        <label>Description</label>
                        <textarea 
                            className="form-input" 
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Optional details about this environment..." 
                            rows="3"
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Registering...' : 'Register Instance'}
                        </button>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)} disabled={submitting}>
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
