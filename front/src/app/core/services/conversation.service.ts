import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Conversation } from '../models/conversation.model';

/** Accès à l'API REST des conversations. */
@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly http = inject(HttpClient);

  /** Liste les conversations, de la plus récente à la plus ancienne (`GET /api/conversations`). */
  list(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>('/api/conversations');
  }

  /** Crée une conversation, sans corps de requête (`POST /api/conversations`). */
  create(): Observable<Conversation> {
    return this.http.post<Conversation>('/api/conversations', null);
  }
}
