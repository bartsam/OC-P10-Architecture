import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';

import { ChatMode } from '../../core/models/chat-mode.model';
import { Message } from '../../core/models/message.model';
import { ChatService } from '../../core/services/chat.service';
import { UserService } from '../../core/services/user.service';

/**
 * Vue tchat, utilisée pour le client (`/chat/:id`) et pour l'agent (`/agent/:id`).
 */
@Component({
  selector: 'app-chat',
  imports: [DatePipe, RouterLink],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class Chat {
  /** Identifiant de la conversation (param `:id` de la route) */
  readonly id = input.required<string>();
  /** Vue ouverte : client ou agent (param `:mode` de la route) */
  readonly mode = input.required<ChatMode>();

  private readonly chatService = inject(ChatService);
  private readonly userService = inject(UserService);

  protected readonly messages = signal<Message[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  /** Utilisateur de démo correspondant à la vue ouverte. */
  protected readonly currentUser = computed(() =>
    this.mode() === 'customer' ? this.userService.customer() : this.userService.agent(),
  );

  constructor() {
    // Recharge l'historique à chaque changement d'id
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
          this.messages.set([]);
        }),
        // switchMap ignore les réponses périmées
        switchMap((id) =>
          this.chatService.getMessages(id).pipe(
            catchError((error: HttpErrorResponse) => {
              this.errorMessage.set(
                error.status === 404
                  ? 'Conversation introuvable.'
                  : 'Impossible de charger la conversation.',
              );
              return of<Message[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((messages) => {
        this.messages.set(messages);
        this.loading.set(false);
      });
  }

  /** Indique si le message a été écrit par l'utilisateur de la vue */
  protected isMine(message: Message): boolean {
    return message.senderId === this.currentUser()?.id;
  }

  /** Label de l'auteur selon `senderId` et mode de la vue. */
  protected authorLabel(message: Message): string {
    if (this.mode() === 'customer') {
      return this.isMine(message) ? 'Vous (client)' : 'Conseiller';
    }
    return this.isMine(message) ? 'Vous (conseiller)' : 'Client';
  }
}
