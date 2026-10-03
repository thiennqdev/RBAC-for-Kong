import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import keycloak from '../config/keycloak';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [loading, setLoading] = useState(true);
    const initCalled = useRef(false);

    useEffect(() => {
        if (initCalled.current) return;
        initCalled.current = true;



        keycloak
            .init({ onLoad: 'login-required', checkLoginIframe: false })
            .then((authenticated) => {
                setIsAuthenticated(authenticated);
                if (authenticated) {
                    setToken(keycloak.token);
                    console.log('[AUTH] Logged in as:', keycloak.tokenParsed?.preferred_username,
                        '| Roles:', keycloak.tokenParsed?.realm_access?.roles);

                    setUser({
                        username: keycloak.tokenParsed?.preferred_username,
                        email: keycloak.tokenParsed?.email,
                        name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username,
                        roles: keycloak.tokenParsed?.realm_access?.roles || [],
                    });

                    // Auto-refresh token
                    setInterval(() => {
                        keycloak
                            .updateToken(30)
                            .then((refreshed) => {
                                if (refreshed) {
                                    setToken(keycloak.token);
                                }
                            })
                            .catch(() => {
                                keycloak.logout();
                            });
                    }, 30000);
                }
                setLoading(false);
            })
            .catch((err) => {
                console.error('Keycloak init failed:', err);
                setLoading(false);
            });
    }, []);

    const logout = useCallback(() => {
        keycloak.logout({ redirectUri: window.location.origin });
    }, []);

    const getToken = useCallback(() => {
        return keycloak.token;
    }, []);

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, token, loading, logout, getToken }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
