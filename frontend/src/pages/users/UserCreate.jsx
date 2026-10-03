import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import userService from '../../services/userService';
import { useToast } from '../../components/common/Toast';

export default function UserCreate() {
    const [form, setForm] = useState({ username: '', email: '', firstName: '', lastName: '', password: '' });
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();

    function handleChange(e) {
        setForm({ ...form, [e.target.name]: e.target.value });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setSubmitting(true);
        try {
            await userService.create(form);
            toast.success('User created successfully!');
            navigate('/users');
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
                    <h2>Create User</h2>
                    <p>Create a new user with Keycloak account</p>
                </div>
            </div>

            <div className="card" style={{ maxWidth: '600px' }}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Username *</label>
                        <input className="form-input" name="username" value={form.username} onChange={handleChange} required minLength={3} placeholder="e.g. john_doe" />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Email *</label>
                        <input className="form-input" name="email" type="email" value={form.email} onChange={handleChange} required placeholder="e.g. john@example.com" />
                    </div>
                    <div className="form-row">
                        <div className="form-group">
                            <label className="form-label">First Name</label>
                            <input className="form-input" name="firstName" value={form.firstName} onChange={handleChange} placeholder="John" />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Last Name</label>
                            <input className="form-input" name="lastName" value={form.lastName} onChange={handleChange} placeholder="Doe" />
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="form-label">Password *</label>
                        <input className="form-input" name="password" type="password" value={form.password} onChange={handleChange} required minLength={6} placeholder="Minimum 6 characters" />
                    </div>
                    <div className="form-actions">
                        <button type="button" className="btn btn-secondary" onClick={() => navigate('/users')}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Creating...' : 'Create User'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
