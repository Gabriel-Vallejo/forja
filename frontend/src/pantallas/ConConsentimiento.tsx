import { useRouter } from 'expo-router';
import type { ReactNode } from 'react';

import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { Texto } from '../componentes/Texto';
import { useConsentimientoLimitaciones } from '../limitaciones/consultas';

interface PropsConConsentimiento {
  /** Qué enseñar sin consentimiento; por defecto, un aviso con la vuelta a Limitaciones. */
  sin?: ReactNode;
  children: ReactNode;
}

function PideConsentimiento() {
  const router = useRouter();
  return (
    <>
      <Texto>Para usar esta parte necesitas dar antes tu consentimiento para guardar limitaciones.</Texto>
      <Boton etiqueta="Ir a Limitaciones" onPress={() => router.replace('/limitaciones')} />
    </>
  );
}

/** El backend exige el consentimiento de limitaciones en todo esto: sin él ni se pide, ni se enseña. */
export function ConConsentimiento({ sin, children }: PropsConConsentimiento) {
  const consentimiento = useConsentimientoLimitaciones();
  return <ConConsulta consulta={consentimiento}>{(dado) => (dado ? children : (sin ?? <PideConsentimiento />))}</ConConsulta>;
}
