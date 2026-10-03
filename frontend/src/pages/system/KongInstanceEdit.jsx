import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import kongInstanceService from '../../services/kongInstanceService';
import { useToast } from '../../components/common/Toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function KongInstanceEdit() {
    const { id } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const [submitting, setSubmitting] = useState(false);
    const [loading, setLoading] = useState(true);
    const [formData, setFormData] = useState({
        code: '',
        name: '',
        adminUrl: '',
        description: '',
        status: 'ACTIVE'
    });

    useEffect(() => {
        loadInstance();
    }, [id]);

    const loadInstance = async () => {
        try {
            const res = await kongInstanceService.getInstance(id);
            setFormData({
                code: res.data.code || '',
                name: res.data.name || '',
                adminUrl: res.data.adminUrl || '',
                description: res.data.description || '',
                status: res.data.status || 'ACTIVE'
            });
        } catch (err) {
            toast.error('Failed to load instance details');
            navigate('/kong-instances');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await kongInstanceService.updateInstance(id, formData);
            toast.success('Kong Instance updated successfully!');
            navigate('/kong-instances');
        } catch (err) {
            toast.error(err.response?.data?.message || err.message || 'Failed to update Kong Instance');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Edit Kong Instance</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Update API Gateway environment settings</p>
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
                            disabled 
                            title="Code cannot be modified"
                        />
                        <small style={{ color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>Environment code cannot be changed.</small>
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

                    <div className="form-group">
                        <label>Status</label>
                        <select 
                            className="form-input" 
                            name="status"
                            value={formData.status}
                            onChange={handleChange}
                        >
                            <option value="ACTIVE">Active</option>
                            <option value="INACTIVE">Inactive</option>
                        </select>
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Updating...' : 'Update Instance'}
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
