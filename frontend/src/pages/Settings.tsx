import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Navbar } from '../components/layout/Navbar';
import { cn } from '../utils/cn';

export default function Settings() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [user, setUser] = useState({ id: '1', nickname: 'John Doe', avatarUrl: '' });
  const [showResetModal, setShowResetModal] = useState(false);

  const [settings, setSettings] = useState({
    notifications: {
      newMessages: true,
      roomInvites: true,
      mentions: true,
      email: false
    },
    privacy: {
      showOnlineStatus: true,
      showLastSeen: true,
      allowDirectMessages: true,
      showInRoomList: true
    },
    appearance: {
      theme: 'dark',
      fontSize: 'medium',
      compactMode: false,
      showTimestamps: true
    },
    chat: {
      autoScroll: true,
      showTypingIndicator: true,
      soundEnabled: true,
      enterToSend: true
    }
  });

  const handleSettingChange = (category: string, setting: string, value: boolean | string) => {
    setSettings(prev => ({
      ...prev,
      [category]: {
        ...prev[category as keyof typeof prev],
        [setting]: value
      }
    }));
  };

  const handleResetSettings = () => {
    setSettings({
      notifications: {
        newMessages: true,
        roomInvites: true,
        mentions: true,
        email: false
      },
      privacy: {
        showOnlineStatus: true,
        showLastSeen: true,
        allowDirectMessages: true,
        showInRoomList: true
      },
      appearance: {
        theme: 'dark',
        fontSize: 'medium',
        compactMode: false,
        showTimestamps: true
      },
      chat: {
        autoScroll: true,
        showTypingIndicator: true,
        soundEnabled: true,
        enterToSend: true
      }
    });
    setShowResetModal(false);
    toast.success('Settings reset to defaults');
  };

  const handleSave = () => {
    toast.success('Settings saved successfully!');
  };

  const handleLogin = () => {
    setIsLoggedIn(true);
    setUser({ id: '1', nickname: 'John Doe', avatarUrl: '' });
    toast.success("Successfully logged in!");
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUser({ id: '1', nickname: 'Guest User', avatarUrl: '' });
    toast.success("Successfully logged out!");
  };

  return (
    <div className="min-h-screen bg-background-primary">
      <Navbar 
        user={isLoggedIn ? user : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />

      <main className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-3xl font-bold text-foreground-primary mb-2">
                Settings
              </h1>
              <p className="text-foreground-muted">
                Customize your VibeChat experience
              </p>
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/')}
            >
              Back to Home
            </Button>
          </div>

          <div className="space-y-6">
            {/* Notifications */}
            <Card className="p-6">
              <h2 className="text-xl font-semibold text-foreground-primary mb-6">
                Notifications
              </h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">New Messages</div>
                    <div className="text-sm text-foreground-muted">Get notified when you receive new messages</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.notifications.newMessages}
                      onChange={(e) => handleSettingChange('notifications', 'newMessages', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Room Invites</div>
                    <div className="text-sm text-foreground-muted">Get notified when you're invited to a room</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.notifications.roomInvites}
                      onChange={(e) => handleSettingChange('notifications', 'roomInvites', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Mentions</div>
                    <div className="text-sm text-foreground-muted">Get notified when someone mentions you</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.notifications.mentions}
                      onChange={(e) => handleSettingChange('notifications', 'mentions', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Email Notifications</div>
                    <div className="text-sm text-foreground-muted">Receive notifications via email</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.notifications.email}
                      onChange={(e) => handleSettingChange('notifications', 'email', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>
              </div>
            </Card>

            {/* Privacy */}
            <Card className="p-6">
              <h2 className="text-xl font-semibold text-foreground-primary mb-6">
                Privacy
              </h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Show Online Status</div>
                    <div className="text-sm text-foreground-muted">Let others see when you're online</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.privacy.showOnlineStatus}
                      onChange={(e) => handleSettingChange('privacy', 'showOnlineStatus', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Show Last Seen</div>
                    <div className="text-sm text-foreground-muted">Let others see when you were last active</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.privacy.showLastSeen}
                      onChange={(e) => handleSettingChange('privacy', 'showLastSeen', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Allow Direct Messages</div>
                    <div className="text-sm text-foreground-muted">Let others send you direct messages</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.privacy.allowDirectMessages}
                      onChange={(e) => handleSettingChange('privacy', 'allowDirectMessages', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>
              </div>
            </Card>

            {/* Appearance */}
            <Card className="p-6">
              <h2 className="text-xl font-semibold text-foreground-primary mb-6">
                Appearance
              </h2>
              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-foreground-primary mb-3">
                    Theme
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <label className="flex items-center p-3 border border-border-default rounded-lg cursor-pointer hover:bg-background-tertiary/50 transition-colors">
                      <input
                        type="radio"
                        name="theme"
                        value="dark"
                        checked={settings.appearance.theme === 'dark'}
                        onChange={(e) => handleSettingChange('appearance', 'theme', e.target.value)}
                        className="mr-3"
                      />
                      <div>
                        <div className="font-medium text-foreground-primary">Dark</div>
                        <div className="text-sm text-foreground-muted">Easy on the eyes</div>
                      </div>
                    </label>
                    <label className="flex items-center p-3 border border-border-default rounded-lg cursor-pointer hover:bg-background-tertiary/50 transition-colors">
                      <input
                        type="radio"
                        name="theme"
                        value="light"
                        checked={settings.appearance.theme === 'light'}
                        onChange={(e) => handleSettingChange('appearance', 'theme', e.target.value)}
                        className="mr-3"
                      />
                      <div>
                        <div className="font-medium text-foreground-primary">Light</div>
                        <div className="text-sm text-foreground-muted">Clean and bright</div>
                      </div>
                    </label>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-foreground-primary mb-3">
                    Font Size
                  </label>
                  <div className="grid grid-cols-3 gap-3">
                    {['small', 'medium', 'large'].map((size) => (
                      <label key={size} className="flex items-center p-3 border border-border-default rounded-lg cursor-pointer hover:bg-background-tertiary/50 transition-colors">
                        <input
                          type="radio"
                          name="fontSize"
                          value={size}
                          checked={settings.appearance.fontSize === size}
                          onChange={(e) => handleSettingChange('appearance', 'fontSize', e.target.value)}
                          className="mr-3"
                        />
                        <div className="font-medium text-foreground-primary capitalize">{size}</div>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Compact Mode</div>
                    <div className="text-sm text-foreground-muted">Reduce spacing for more content</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.appearance.compactMode}
                      onChange={(e) => handleSettingChange('appearance', 'compactMode', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>
              </div>
            </Card>

            {/* Chat Settings */}
            <Card className="p-6">
              <h2 className="text-xl font-semibold text-foreground-primary mb-6">
                Chat Settings
              </h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Auto Scroll</div>
                    <div className="text-sm text-foreground-muted">Automatically scroll to new messages</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.chat.autoScroll}
                      onChange={(e) => handleSettingChange('chat', 'autoScroll', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Show Typing Indicator</div>
                    <div className="text-sm text-foreground-muted">Show when others are typing</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.chat.showTypingIndicator}
                      onChange={(e) => handleSettingChange('chat', 'showTypingIndicator', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Sound Effects</div>
                    <div className="text-sm text-foreground-muted">Play sounds for new messages</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.chat.soundEnabled}
                      onChange={(e) => handleSettingChange('chat', 'soundEnabled', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium text-foreground-primary">Enter to Send</div>
                    <div className="text-sm text-foreground-muted">Press Enter to send messages</div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.chat.enterToSend}
                      onChange={(e) => handleSettingChange('chat', 'enterToSend', e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-foreground-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-background-tertiary after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-foreground-primary"></div>
                  </label>
                </div>
              </div>
            </Card>

            {/* Action Buttons */}
            <div className="flex items-center justify-between pt-6">
              <Button
                variant="secondary"
                onClick={() => setShowResetModal(true)}
              >
                Reset to Defaults
              </Button>
              <Button
                variant="primary"
                onClick={handleSave}
              >
                Save Settings
              </Button>
            </div>
          </div>
        </div>
      </main>

      {/* Reset Settings Modal */}
      <Modal isOpen={showResetModal} onClose={() => setShowResetModal(false)}>
        <Modal.Header>
          <Modal.Title>Reset Settings</Modal.Title>
        </Modal.Header>
        <Modal.Content>
          <p className="text-foreground-muted">
            Are you sure you want to reset all settings to their default values? This action cannot be undone.
          </p>
        </Modal.Content>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => setShowResetModal(false)}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleResetSettings}
          >
            Reset Settings
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
