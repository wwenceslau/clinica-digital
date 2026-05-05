import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Ensure DOM is fully cleaned up after every test, even when running multiple
// test files in a single fork (singleFork: true).
afterEach(() => {
  cleanup();
});
