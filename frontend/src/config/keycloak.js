import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: 'http://localhost:8180',
    realm: 'rbac-management',
    clientId: 'rbac-admin-frontend',
});

export default keycloak;
