import { useEffect, useState } from 'react';

export function useDarkMode() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    // Check if theme is stored in localStorage
    const stored = localStorage.getItem('theme') as 'light' | 'dark';
    if (stored) return stored;
    
    // Check system preference
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    const root = window.document.documentElement;
    
    // Remove both classes first
    root.classList.remove('light', 'dark');
    
    // Add the current theme class
    root.classList.add(theme);
    
    // Save to localStorage
    localStorage.setItem('theme', theme);
    
    // Dispatch event for other components to listen
    window.dispatchEvent(new CustomEvent('theme-changed', { detail: theme }));
  }, [theme]);

  // Listen to system preference changes
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    
    const handleChange = (e: MediaQueryListEvent) => {
      // Only auto-change if no user preference is stored
      if (!localStorage.getItem('theme')) {
        setTheme(e.matches ? 'dark' : 'light');
      }
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const toggleDarkMode = () => {
    setTheme(prevTheme => (prevTheme === 'light' ? 'dark' : 'light'));
  };

  const setLightMode = () => setTheme('light');
  const setDarkMode = () => setTheme('dark');

  return {
    theme,
    isDarkMode: theme === 'dark',
    isLightMode: theme === 'light',
    toggleDarkMode,
    setLightMode,
    setDarkMode,
    // Legacy support
    toggleTheme: toggleDarkMode
  };
}
