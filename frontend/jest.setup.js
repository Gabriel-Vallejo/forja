/* global jest */
// Mocks oficiales de gesture-handler, Worklets y Reanimated para Jest (con el resolver de Reanimated en package.json).
require('react-native-gesture-handler/jestSetup');
jest.mock('react-native-worklets', () => require('react-native-worklets/lib/module/mock'));
require('react-native-reanimated').setUpTests();
