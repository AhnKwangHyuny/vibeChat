import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Home from '../pages/Home';
import Room from '../pages/Room';
import RoomDemo from '../pages/RoomDemo';
import CreateRoom from '../pages/CreateRoom';
import Profile from '../pages/Profile';
import Settings from '../pages/Settings';
import Components from '../pages/Components';
import RoomList from '../pages/RoomList';
import NotFound from '../pages/NotFound';
import ErrorPage from '../pages/ErrorPage';
import AuthCallback from '../pages/AuthCallback';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Home />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/rooms/:roomId',
    element: <Room />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/create',
    element: <CreateRoom />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/profile',
    element: <Profile />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/settings',
    element: <Settings />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/components',
    element: <Components />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/roomList',
    element: <RoomList />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/room/demo',
    element: <RoomDemo />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/test-room',
    element: <Room />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/room/:roomId',
    element: <Room />,
    errorElement: <ErrorPage />,
  },
  {
    path: '/auth/callback',
    element: <AuthCallback />,
    errorElement: <ErrorPage />,
  },
  {
    path: '*',
    element: <NotFound />
  }
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}