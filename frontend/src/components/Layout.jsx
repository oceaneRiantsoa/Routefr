// frontend/src/components/Layout.jsx
import React, { useState } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { logout, getCurrentUser } from '../services/authService';
import CreateUserModal from './CreateUserModal';
import './Layout.css';

const Layout = ({ children }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const user = getCurrentUser();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [showCreateUserModal, setShowCreateUserModal] = useState(false);

    const handleLogout = async () => {
        if (window.confirm('Voulez-vous vraiment vous déconnecter ?')) {
            await logout();
            navigate('/manager/login');
        }
    };

    const handleUserCreated = (newUser) => {
        console.log('Nouvel utilisateur créé:', newUser);
    };

    const getPageTitle = () => {
        switch (location.pathname) {
            case '/manager':
                return 'Tableau de bord';
            case '/manager/signalements':
                return 'Gestion des Signalements';
            case '/manager/statistiques':
                return 'Statistiques';
            case '/manager/users':
                return 'Utilisateurs';
            case '/manager/sync':
                return 'Synchronisation';
            default:
                return 'Manager';
        }
    };

    const menuItems = [
        { path: '/manager', label: 'Tableau de bord', exact: true },
        { path: '/manager/signalements', label: 'Signalements' },
        { path: '/manager/statistiques', label: 'Statistiques' },
        { path: '/manager/sync', label: 'Synchronisation' },
        { path: '/manager/users', label: 'Utilisateurs' }
    ];

    return (
        <div className={`layout ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
            <aside className="sidebar">
                <div className="sidebar-header">
                    <div className="logo">
                        <span className="logo-text">RouteManager</span>
                    </div>
                    <button
                        className="sidebar-toggle"
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                    >
                        {sidebarCollapsed ? '»' : '«'}
                    </button>
                </div>

                <nav className="sidebar-nav">
                    {menuItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            end={item.exact}
                            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                        >
                            <span className="nav-label">{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                <div className="sidebar-footer">
                    <NavLink to="/" className="nav-item map-link">
                        <span className="nav-label">Voir la carte</span>
                    </NavLink>
                </div>
            </aside>

            <div className="main-wrapper">
                <header className="top-header">
                    <div className="header-left">
                        <h1 className="page-title">{getPageTitle()}</h1>
                    </div>
                    <div className="header-right">
                        <button
                            className="btn btn-primary"
                            onClick={() => setShowCreateUserModal(true)}
                        >
                            + Créer utilisateur
                        </button>
                        <div className="user-profile">
                            <div className="user-avatar">
                                {(user?.displayName || user?.email || 'M').charAt(0).toUpperCase()}
                            </div>
                            <span className="user-name">{user?.displayName || user?.email || 'Manager'}</span>
                        </div>
                        <button className="btn btn-outline" onClick={handleLogout}>
                            Déconnexion
                        </button>
                    </div>
                </header>

                <main className="main-content">
                    {children}
                </main>
            </div>

            <CreateUserModal
                isOpen={showCreateUserModal}
                onClose={() => setShowCreateUserModal(false)}
                onUserCreated={handleUserCreated}
            />
        </div>
    );
};

export default Layout;
