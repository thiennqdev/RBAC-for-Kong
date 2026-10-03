import { useState, useEffect } from 'react';
import { NavLink, useLocation, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import projectService from '../../services/projectService';
import fileService from '../../services/fileService';

const systemAdminNav = [
    { section: 'Overview', items: [{ path: '/', label: 'Dashboard', icon: '📊' }] },
    {
        section: 'System Administration',
        items: [
            { path: '/users', label: 'Users', icon: '👥' },
            { path: '/projects', label: 'Projects', icon: '📁' },
            { path: '/kong-instances', label: 'Kong Instances', icon: '🌐' },
        ],
    },
];

function getProjectNav(projectCode, isSystemAdmin) {
    const generalItems = [
        { path: `/projects/${projectCode}`, label: 'Overview', icon: '📋', end: true },
        { path: `/projects/${projectCode}/members`, label: 'Members', icon: '👥' },
    ];

    const kongApiItems = [];
    if (!isSystemAdmin) {
        kongApiItems.push({ path: `/projects/${projectCode}/kong`, label: 'API Gateway', icon: '🌐' });
        kongApiItems.push({ path: `/projects/${projectCode}/live-logs`, label: 'Live Logs', icon: '📡' });
        kongApiItems.push({ path: `/projects/${projectCode}/gateway-logs`, label: 'Gateway Logs', icon: '🗄️' });
        kongApiItems.push({ path: `/projects/${projectCode}/audit-logs`, label: 'Audit Logs', icon: '📝' });
    }

    const fileManagementItems = [];
    if (!isSystemAdmin) {
        fileManagementItems.push({ path: `/projects/${projectCode}/storage`, label: 'File Browser', icon: '📁' });
    }

    const nav = [
        {
            section: 'General',
            items: generalItems,
        }
    ];

    if (!isSystemAdmin) {
        nav.push({
            section: 'Kong API',
            items: kongApiItems,
        });
        nav.push({
            section: 'File Management',
            items: fileManagementItems,
        });
    }

    nav.push({
        section: 'System',
        items: [
            { path: '/', label: '← Back to Admin', icon: '🏠' },
        ],
    });

    return nav;
}

export default function Sidebar() {
    const location = useLocation();

    // Detect if we're in a project context
    const projectMatch = location.pathname.match(/^\/projects\/([^/]+)/);
    const isProjectContext = projectMatch && location.pathname !== '/projects' && location.pathname !== '/projects/create';
    const projectCode = isProjectContext ? projectMatch[1] : null;
    const { user } = useAuth();
    const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN');
    
    const [projectDetail, setProjectDetail] = useState(null);

    useEffect(() => {
        const load = () => {
            if (projectCode) {
                projectService.get(projectCode).then(res => {
                    setProjectDetail(res.data);
                }).catch(err => console.error("Failed to load project details", err));
            } else {
                setProjectDetail(null);
            }
        };

        load();

        window.addEventListener('project-updated', load);
        return () => window.removeEventListener('project-updated', load);
    }, [projectCode]);

    // Dynamically build systemAdminNav based on role
    const dynamicSystemAdminNav = [
        { section: 'Overview', items: [{ path: '/', label: 'Dashboard', icon: '📊' }] },
        {
            section: isSystemAdmin ? 'System Administration' : 'My Projects',
            items: [
                ...(isSystemAdmin ? [
                    { path: '/users', label: 'Users', icon: '👥' },
                    { path: '/kong-instances', label: 'Kong Instances', icon: '🌐' }
                ] : []),
                { path: '/projects', label: isSystemAdmin ? 'All Projects' : 'My Projects', icon: '📁' },
            ],
        },
    ];


    const navItems = isProjectContext ? getProjectNav(projectCode, isSystemAdmin) : dynamicSystemAdminNav;

    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                {isProjectContext && projectDetail ? (
                    <>
                        {projectDetail.iconPath ? (
                            <img src={fileService.getFileUrl(projectDetail.iconPath)} alt={projectDetail.projectName} className="logo-icon" style={{ padding: 0, objectFit: 'cover' }} />
                        ) : (
                            <div className="logo-icon">P</div>
                        )}
                        <h1 style={{ fontSize: '1.2rem' }}>{projectDetail.projectName}</h1>
                    </>
                ) : (
                    <>
                        <div className="logo-icon">R</div>
                        <h1>RBAC Admin</h1>
                    </>
                )}
            </div>
            <nav className="sidebar-nav">
                {navItems.map((section) => (
                    <div className="nav-section" key={section.section}>
                        <div className="nav-section-title">{section.section}</div>
                        {section.items.map((item) => (
                            <NavLink
                                key={item.path}
                                to={item.path}
                                end={item.end || item.path === '/'}
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                <span className="nav-icon">{item.icon}</span>
                                {item.label}
                            </NavLink>
                        ))}
                    </div>
                ))}
            </nav>
        </aside>
    );
}
