import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Conversation } from '../../core/models/conversation.model';
import { ChatService } from '../../core/services/chat.service';
import { UserService } from '../../core/services/user.service';

/** Page d'accueil : démarrage d'une conversation et liste des conversations existantes. */
@Component({
  selector: 'app-home',
  imports: [DatePipe, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly loading = signal(true);
  protected readonly creating = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly userError = inject(UserService).error;

  constructor() {
    this.loadConversations();
  }

  /** Crée une conversation, puis ouvre sa vue tchat client. */
  protected startConversation(): void {
    this.creating.set(true);
    this.errorMessage.set(null);
    this.chatService.create().subscribe({
      next: (conversation) => {
        this.creating.set(false);
        void this.router.navigate(['/chat', conversation.id]);
      },
      error: () => {
        this.errorMessage.set('Impossible de créer la conversation.');
        this.creating.set(false);
      },
    });
  }

  /** Charge les conversations existantes (les requêtes HTTP se terminent seules, pas de désabonnement). */
  private loadConversations(): void {
    this.chatService.list().subscribe({
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
