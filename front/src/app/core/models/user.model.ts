/** Rôle d'un utilisateur de démonstration. */
export type Role = 'CUSTOMER' | 'AGENT';

/** Utilisateur de démo renvoyé par `GET /api/users`. */
export interface User {
  id: string;
  firstName: string;
  lastName: string;
  role: Role;
}
