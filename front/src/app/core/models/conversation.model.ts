/** Conversation renvoyée par `/api/conversations`. */
export interface Conversation {
  id: string;
  userId: string;
  /** Date ISO-8601 fixée par le serveur. */
  createdAt: string;
}
