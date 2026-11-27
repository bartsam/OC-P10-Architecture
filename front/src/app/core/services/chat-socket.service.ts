import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable } from 'rxjs';

import { ChatEvent } from '../models/chat.model';
import { Message } from '../models/message.model';

/**
 * Connexion STOMP d'une conversation.
 * (une instance par vue, détruite avec elle).
 */
@Injectable()
export class ChatSocketService {
  private readonly location = inject(DOCUMENT).location;
  private client: Client | undefined;

  /**
   * Ouvre la connexion et émet les événements de la conversation.
   * - S'abonner active le client (avec reconnexion auto), se désabonner le désactive.
   * - Sur chaque connexion, l'abonnement au topic précède l'événement `connected` :
   *   le composant peut alors charger l'historique sans risquer de perdre un message.
   */
  watch(conversationId: string): Observable<ChatEvent> {
    return new Observable<ChatEvent>((subscriber) => {
      const client = new Client({
        brokerURL: this.brokerUrl(),
        reconnectDelay: 3000,
        onConnect: () => {
          client.subscribe(`/topic/conversations/${conversationId}`, (frame) =>
            subscriber.next({ type: 'message', message: JSON.parse(frame.body) as Message }),
          );
          subscriber.next({ type: 'connected' });
        },
        onWebSocketClose: () => subscriber.next({ type: 'disconnected' }),
      });
      this.client = client;
      client.activate();

      return () => {
        void client.deactivate();
        if (this.client === client) {
          this.client = undefined;
        }
      };
    });
  }

  /**
   * Envoie un message vers `/app/conversations/{id}/messages`.
   * Lève une erreur si la connexion n'est pas établie.
   */
  send(conversationId: string, senderId: string, content: string): void {
    if (!this.client?.connected) {
      throw new Error('Connexion STOMP indisponible');
    }
    this.client.publish({
      destination: `/app/conversations/${conversationId}/messages`,
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ senderId, content }),
    });
  }

  /** URL WebSocket relative à la page : `/ws` passe par le proxy de développement. */
  private brokerUrl(): string {
    const scheme = this.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${scheme}://${this.location.host}/ws`;
  }
}
