import React, { useEffect, useState, useContext } from 'react';
import api from '../../services/api';
import { KongContext } from '../../context/KongContext';
import { useToast } from '../common/Toast';

export default function KongInstanceSelector() {
    const { selectedKongInstance, setSelectedKongInstance } = useContext(KongContext);
    const [instances, setInstances] = useState([]);
    const [loading, setLoading] = useState(true);
    const toast = useToast();

    useEffect(() => {
        const fetchInstances = async () => {
            try {
                const res = await api.get('/kong-instances');
                setInstances(res.data);
                
                // If there is no selected instance and we have instances available, select the first one
                if (!selectedKongInstance && res.data.length > 0) {
                    setSelectedKongInstance(res.data[0]);
                } else if (selectedKongInstance) {
                    // Make sure the selected instance still exists in the fetched list
                    const exists = res.data.find(i => i.id === selectedKongInstance.id);
                    if (!exists && res.data.length > 0) {
                        setSelectedKongInstance(res.data[0]);
                    }
                }
            } catch (err) {
                toast.error('Failed to load Kong instances');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchInstances();
    }, [selectedKongInstance, setSelectedKongInstance, toast]);

    const handleSelectChange = (e) => {
        const instanceId = parseInt(e.target.value, 10);
        const instance = instances.find(i => i.id === instanceId);
        setSelectedKongInstance(instance);
    };

    if (loading) {
        return <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Loading instances...</div>;
    }

    if (instances.length === 0) {
        return <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>No Kong instances available</div>;
    }

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Kong Environment:</label>
            <select 
                className="form-input" 
                style={{ padding: '4px 8px', fontSize: '0.85rem', height: 'auto', minWidth: '150px' }}
                value={selectedKongInstance ? selectedKongInstance.id : ''}
                onChange={handleSelectChange}
            >
                {instances.map(instance => (
                    <option key={instance.id} value={instance.id}>
                        {instance.name} ({instance.code})
                    </option>
                ))}
            </select>
        </div>
    );
}
