import { useAuth } from '../../context/AuthContext';
import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const pageTitles = {
    '/': 'Dashboard',
    '/users': 'User Management',
    '/projects': 'Project Management',
};

function getPageTitle(pathname) {
    if (pageTitles[pathname]) return pageTitles[pathname];
    const basePath = '/' + (pathname.split('/')[1] || '');
    if (pageTitles[basePath]) return pageTitles[basePath];
    if (pathname.match(/\/projects\/[^/]+\/roles/)) return 'Role Management';
    if (pathname.match(/\/projects\/[^/]+\/permissions/)) return 'Resources & Permissions';
    if (pathname.match(/\/projects\/[^/]+\/members/)) return 'Project Members';
    if (pathname.match(/\/projects\/[^/]+\/providers/)) return 'Provider Integration';
    if (pathname.match(/\/projects\/[^/]+/)) return 'Project Details';
    return 'RBAC Admin';
}

export default function Header() {
    const { user, logout } = useAuth();
    const location = useLocation();
    const { t, i18n } = useTranslation();

    const title = getPageTitle(location.pathname);

    const toggleLanguage = () => {
        const newLang = i18n.language === 'en' ? 'vi' : 'en';
        i18n.changeLanguage(newLang);
    };

    return (
        <header className="header">
            <h2 className="header-title">{t(title)}</h2>
            <div className="header-user">
                <button onClick={toggleLanguage} className="btn btn-secondary btn-sm" style={{ marginRight: '16px', padding: '4px 8px' }}>
                    {i18n.language === 'en' ? '🇻🇳 VI' : '🇬🇧 EN'}
                </button>
                <div className="user-avatar">
                    {user?.name?.charAt(0)?.toUpperCase() || 'A'}
                </div>
                <span className="user-name">{user?.name || 'Admin'}</span>
                <button className="btn-logout" onClick={logout}>
                    {t('Logout')}
                </button>
            </div>
        </header>
    );
}
