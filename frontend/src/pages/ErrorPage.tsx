import React from 'react';
import { useRouteError, isRouteErrorResponse } from 'react-router-dom';

export default function ErrorPage() {
  const error = useRouteError();
  console.error(error);

  let errorMessage: string;

  if (isRouteErrorResponse(error)) {
    errorMessage = error.statusText || error.data?.message || error.data;
  } else if (error instanceof Error) {
    errorMessage = error.message;
  } else if (typeof error === 'string') {
    errorMessage = error;
  } else {
    errorMessage = 'An unexpected error has occurred.';
  }

  return (
    <div id="error-page" className="flex flex-col items-center justify-center min-h-screen bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white">
      <h1 className="text-4xl font-bold mb-4">Oops!</h1>
      <p className="text-lg mb-2">Sorry, an unexpected error has occurred.</p>
      <p className="text-md text-gray-600 dark:text-gray-400">
        <i>{errorMessage}</i>
      </p>
      <button
        onClick={() => window.history.back()}
        className="mt-6 px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600"
      >
        Go Back
      </button>
    </div>
  );
}
