import { Message } from './message.model';

/** Vue du tchat : détermine l'identité simulée (client ou agent). */
export type ChatMode = 'customer' | 'agent';

/** État de la connexion WebSocket affiché à l'utilisateur. */
export type ConnectionState = 'connecting' | 'connected' | 'reconnecting';

/** Événement émis par la connexion temps réel d'une conversation. */
export type ChatEvent =
  { type: 'connected' } | { type: 'disconnected' } | { type: 'message'; message: Message };
