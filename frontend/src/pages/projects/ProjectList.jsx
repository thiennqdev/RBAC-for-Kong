import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import projectService from '../../services/projectService';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';

export default function ProjectList() {
    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const navigate = useNavigate();
    const toast = useToast();
    const { user } = useAuth();
    const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN');

    useEffect(() => { loadProjects(); }, [page, search]);

    async function loadProjects() {
        setLoading(true);
        try {
            const params = { page, size: 10 };
            if (search) params.keyword = search;
            const res = await projectService.list(params);
            const data = res.data;
            setProjects(data.data || data.content || []);
            setTotalPages(data.totalPages || 1);
        } catch (err) {
            toast.error('Failed to load projects: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>{isSystemAdmin ? 'All Projects' : 'My Projects'}</h2>
                    <p>{isSystemAdmin ? 'Manage multi-tenant projects with schema isolation' : 'View and access your assigned projects'}</p>
                </div>
                {isSystemAdmin && (
                    <button className="btn btn-primary" onClick={() => navigate('/projects/create')}>+ Create Project</button>
                )}
            </div>

            <div className="toolbar">
                <input type="text" className="search-input" placeholder="🔍 Search projects..." value={search}
                    onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
            </div>

            {loading ? <LoadingSpinner /> : (
                <div className="data-table-wrapper">
                    {projects.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-icon">📁</div>
                            <h3>No projects found</h3>
                        </div>
                    ) : (
                        <>
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>Code</th>
                                        <th>Name</th>
                                        <th>Status</th>
                                        <th>Created By</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {projects.map((p) => (
                                        <tr key={p.projectCode} style={{ opacity: p.status === 'INACTIVE' ? 0.6 : 1 }}>
                                            <td style={{ color: 'var(--accent-primary)', fontWeight: 600, fontFamily: 'monospace' }}>{p.projectCode}</td>
                                            <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.projectName}</td>
                                            <td><StatusBadge status={p.status} /></td>
                                            <td>{p.createdBy || '-'}</td>
                                            <td>
                                                <button className="btn btn-sm btn-secondary" onClick={() => navigate(`/projects/${p.projectCode}`)}>
                                                    View →
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {totalPages > 1 && (
                                <div className="pagination">
                                    <button disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
                                    {Array.from({ length: totalPages }, (_, i) => (
                                        <button key={i} className={page === i ? 'active' : ''} onClick={() => setPage(i)}>{i + 1}</button>
                                    ))}
                                    <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>→</button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
