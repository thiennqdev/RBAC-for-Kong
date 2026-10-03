import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import kongInstanceService from '../../services/kongInstanceService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function KongInstanceList() {
    const navigate = useNavigate();
    const toast = useToast();
    const [instances, setInstances] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadInstances();
    }, []);

    const loadInstances = async () => {
        try {
            const res = await kongInstanceService.listInstances();
            setInstances(res.data || []);
        } catch (err) {
            toast.error('Failed to load Kong Instances: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this Kong Instance? This action cannot be undone.')) return;
        try {
            await kongInstanceService.deleteInstance(id);
            toast.success('Kong Instance deleted successfully');
            loadInstances();
        } catch (err) {
            toast.error('Failed to delete instance: ' + err.message);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Kong Instances</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage API Gateway environments for the system</p>
                </div>
                <button className="btn btn-primary" onClick={() => navigate('create')}>+ Register Instance</button>
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Name</th>
                            <th>Admin URL</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {instances.length === 0 ? (
                            <tr><td colSpan="4" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>No Kong Instances registered</td></tr>
                        ) : (
                            instances.map(inst => (
                                <tr key={inst.id}>
                                    <td style={{ fontWeight: 600 }}>{inst.code}</td>
                                    <td>{inst.name}</td>
                                    <td>
                                        <code style={{ background: 'var(--bg-secondary)', padding: '4px 8px', borderRadius: '4px', fontSize: '0.85em' }}>
                                            {inst.adminUrl}
                                        </code>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: inst.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' : 'rgba(107,114,128,0.15)',
                                            color: inst.status === 'ACTIVE' ? '#22c55e' : '#6b7280',
                                        }}>
                                            {inst.status}
                                        </span>
                                    </td>
                                    <td>
                                        <button 
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => navigate(`/kong-instances/${inst.id}/edit`)}
                                            style={{ padding: '4px 10px', fontSize: '0.85em', marginRight: '8px' }}
                                        >
                                            Edit
                                        </button>
                                        <button 
                                            className="btn btn-danger btn-sm"
                                            onClick={() => handleDelete(inst.id)}
                                            style={{ padding: '4px 10px', fontSize: '0.85em' }}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
