import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { Conversation } from '../../core/models/conversation.model';
import { ConversationService } from '../../core/services/conversation.service';
import { UserService } from '../../core/services/user.service';

/** Page d'accueil : démarrage d'une conversation et liste des conversations existantes. */
@Component({
  selector: 'app-home',
  imports: [DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly conversationService = inject(ConversationService);

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly loading = signal(true);
  protected readonly creating = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly userError = inject(UserService).error;

  constructor() {
    this.loadConversations();
  }

  /** Crée une conversation et l'ajoute en tête de liste */
  protected startConversation(): void {
    this.creating.set(true);
    this.errorMessage.set(null);
    this.conversationService.create().subscribe({
      next: (conversation) => {
        this.conversations.update((list) => [conversation, ...list]);
        this.creating.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de créer la conversation.');
        this.creating.set(false);
      },
    });
  }

  /** Charge les conversations existantes (les requêtes HTTP se terminent seules, pas de désabonnement). */
  private loadConversations(): void {
    this.conversationService.list().subscribe({
      next: (conversations) => {
        this.conversations.set(conversations);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les conversations. Le back est-il démarré ?');
        this.loading.set(false);
      },
    });
  }
}
