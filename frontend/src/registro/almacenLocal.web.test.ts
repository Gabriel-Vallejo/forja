import { abrirAlmacenLocal } from './almacenLocal.web';

test('en web no hay almacén local: devuelve null sin romper', async () => {
  await expect(abrirAlmacenLocal()).resolves.toBeNull();
});
