import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { User } from '../models/user.model';

/**
 * Utilisateurs de démo : la vue ouverte (client ou agent)
 * détermine l'identité simulée (il n'y a pas d'authentification)
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly users = signal<User[]>([]);
  private readonly errorMessage = signal<string | null>(null);

  /** Utilisateur de démonstration utilisé par la vue client. */
  readonly customer = computed(() => this.users().find((user) => user.role === 'CUSTOMER'));
  /** Utilisateur de démonstration utilisé par la vue agent. */
  readonly agent = computed(() => this.users().find((user) => user.role === 'AGENT'));
  /** Message d'erreur affichable, ou `null` si le chargement a réussi. */
  readonly error = this.errorMessage.asReadonly();

  /** Charge les utilisateurs depuis `GET /api/users` (appelé au démarrage de l'application). */
  load(): void {
    this.http.get<User[]>('/api/users').subscribe({
      next: (users) => this.users.set(users),
      error: () =>
        this.errorMessage.set(
          'Impossible de charger les utilisateurs de démonstration. Le back est-il démarré ?',
        ),
    });
  }
}
