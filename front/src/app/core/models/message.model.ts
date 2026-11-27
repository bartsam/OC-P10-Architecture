/** Message renvoyé par `GET /api/conversations/{id}/messages`. */
export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  /** Date ISO-8601 fixée par le serveur. */
  sentAt: string;
}
