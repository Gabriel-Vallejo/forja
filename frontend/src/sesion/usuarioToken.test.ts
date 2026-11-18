import { tokenDe } from '../pruebas/dobles';
import { usuarioDeToken } from './usuarioToken';

const USUARIO = '0193a3c4-7b2e-7c1d-9f00-00000000abcd';

function jwt(carga: object): string {
  const b64url = (texto: string) => btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url('{"alg":"EdDSA"}')}.${b64url(JSON.stringify(carga))}.firma`;
}

describe('usuarioDeToken', () => {
  test('lee el sub del token de acceso', () => {
    expect(usuarioDeToken(jwt({ sub: USUARIO, sid: 'x' }))).toBe(USUARIO);
    expect(usuarioDeToken(tokenDe(USUARIO))).toBe(USUARIO);
  });

  test('devuelve null si el token no es un JWT o no trae un sub con forma de UUID', () => {
    expect(usuarioDeToken('acceso-1')).toBeNull();
    expect(usuarioDeToken('a.no-es-base64-json.c')).toBeNull();
    expect(usuarioDeToken(jwt({ sid: 'x' }))).toBeNull();
    expect(usuarioDeToken(jwt({ sub: 'pepe' }))).toBeNull();
  });
});
