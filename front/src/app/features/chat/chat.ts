import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';

import { ChatEvent, ChatMode, ConnectionState } from '../../core/models/chat.model';
import { Message } from '../../core/models/message.model';
import { ChatApiService } from '../../core/services/chat-api.service';
import { ChatSocketService } from '../../core/services/chat-socket.service';
import { UserService } from '../../core/services/user.service';

/** Longueur maximale d'un message, identique à la contrainte du back. */
const MAX_LENGTH = 2000;

/**
 * Vue tchat, utilisée pour le client (`/chat/:id`) et pour l'agent (`/agent/:id`).
 */
@Component({
  selector: 'app-chat',
  imports: [DatePipe, RouterLink],
  providers: [ChatSocketService],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class Chat {
  /** Identifiant de la conversation (paramètre `:id` de la route). */
  readonly id = input.required<string>();
  /** Vue ouverte : client ou agent (donnée `mode` de la route). */
  readonly mode = input.required<ChatMode>();

  private readonly chatApiService = inject(ChatApiService);
  private readonly chatSocketService = inject(ChatSocketService);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly messages = signal<Message[]>([]);
  protected readonly loading = signal(true);
  protected readonly connection = signal<ConnectionState>('connecting');
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sendError = signal<string | null>(null);
  protected readonly draft = signal('');

  /** Utilisateur de démo correspondant à la vue ouverte. */
  protected readonly currentUser = computed(() =>
    this.mode() === 'customer' ? this.userService.customer() : this.userService.agent(),
  );

  /** Label de l'état de la connexion. */
  protected readonly connectionLabel = computed(() => {
    switch (this.connection()) {
      case 'connected':
        return 'Connecté';
      case 'reconnecting':
        return 'Connexion perdue, reconnexion en cours…';
      default:
        return 'Connexion en cours…';
    }
  });

  /** Condition d'envoi du message (statut connecté, 1..2000 caractères) */
  protected readonly canSend = computed(() => {
    const text = this.draft();
    return (
      this.connection() === 'connected' &&
      !!this.currentUser() &&
      text.trim().length > 0 &&
      text.length <= MAX_LENGTH
    );
  });

  constructor() {
    // Une connexion par conversation
    toObservable(this.id)
      .pipe(
        tap(() => this.resetView()),
        // switchMap ferme l'ancienne conversation si l'id change
        switchMap((id) => this.chatSocketService.watch(id)),
        takeUntilDestroyed(),
      )
      .subscribe((event) => this.handleEvent(event));
  }

  /** Valide l'envoi du message saisi puis vide le champ */
  protected send(event: Event): void {
    event.preventDefault();
    const user = this.currentUser();
    if (!this.canSend() || !user) {
      return;
    }
    try {
      this.chatSocketService.send(this.id(), user.id, this.draft());
      this.sendError.set(null);
      this.draft.set('');
    } catch {
      this.sendError.set(
        "Envoi impossible : la connexion est perdue. Votre message n'a pas été envoyé.",
      );
    }
  }

  /** Indique si le message a été écrit par l'utilisateur de la vue */
  protected isMine(message: Message): boolean {
    return message.senderId === this.currentUser()?.id;
  }

  /** Label de l'auteur, selon `senderId` et mode de la vue. */
  protected authorLabel(message: Message): string {
    if (this.mode() === 'customer') {
      return this.isMine(message) ? 'Vous (client)' : 'Conseiller';
    }
    return this.isMine(message) ? 'Vous (conseiller)' : 'Client';
  }

  /** Réinitialise l'affichage quand on change de conversation. */
  private resetView(): void {
    this.messages.set([]);
    this.loading.set(true);
    this.connection.set('connecting');
    this.errorMessage.set(null);
    this.sendError.set(null);
  }

  /** Réagit à un événement de la connexion temps réel. */
  private handleEvent(event: ChatEvent): void {
    switch (event.type) {
      case 'connected':
        this.connection.set('connected');
        this.loadHistory();
        break;
      case 'disconnected':
        this.connection.set('reconnecting');
        break;
      case 'message':
        this.merge([event.message]);
        break;
    }
  }

  /**
   * Charge l'historique de la conversation par REST :
   * - Appelé après chaque connexion ou reconnexion pour récupérer les messages manqués,
   * - Fusion par id pour éviter les doublons.
   */
  private loadHistory(): void {
    const id = this.id();
    this.chatApiService
      .getMessages(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (messages) => {
          if (id !== this.id()) {
            return;
          }
          this.merge(messages);
          this.errorMessage.set(null);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.status === 404
              ? 'Conversation introuvable.'
              : 'Impossible de charger la conversation.',
          );
          this.loading.set(false);
        },
      });
  }

  /** Ajoute des messages à la liste sans doublon (par id), triés par date d'envoi */
  private merge(incoming: Message[]): void {
    this.messages.update((current) => {
      const byId = new Map(current.map((message) => [message.id, message]));
      for (const message of incoming) {
        if (message.conversationId === this.id()) {
          byId.set(message.id, message);
        }
      }
      // Date.parse parce que les chaînes ISO du serveur n'ont pas toutes le même nombre de décimales
      return [...byId.values()].sort((a, b) => Date.parse(a.sentAt) - Date.parse(b.sentAt));
    });
  }
}
