import React, { createContext, useState, useEffect } from 'react';

export const KongContext = createContext();

export const KongProvider = ({ children }) => {
    // Try to load from localStorage first to keep selection across reloads
    const [selectedKongInstance, setSelectedKongInstance] = useState(() => {
        const saved = localStorage.getItem('selectedKongInstance');
        return saved ? JSON.parse(saved) : null;
    });

    useEffect(() => {
        if (selectedKongInstance) {
            localStorage.setItem('selectedKongInstance', JSON.stringify(selectedKongInstance));
        } else {
            localStorage.removeItem('selectedKongInstance');
        }
    }, [selectedKongInstance]);

    return (
        <KongContext.Provider value={{ selectedKongInstance, setSelectedKongInstance }}>
            {children}
        </KongContext.Provider>
    );
};
