import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { vi } from 'vitest';

import { ChatEvent, ChatMode } from '../../core/models/chat.model';
import { ChatSocketService } from '../../core/services/chat-socket.service';
import { UserService } from '../../core/services/user.service';
import { Chat } from './chat';

/** Faux service WebSocket : le test déclenche lui-même les événements. */
class FakeChatSocketService {
  readonly events = new Subject<ChatEvent>();
  readonly send = vi.fn();

  watch() {
    return this.events.asObservable();
  }
}

const demoUsers = [
  { id: 'u-customer', firstName: 'John', lastName: 'Doe', role: 'CUSTOMER' as const },
  { id: 'u-agent', firstName: 'Jean', lastName: 'Biche', role: 'AGENT' as const },
];

const customerMessage = {
  id: 'm1',
  conversationId: 'conv-1',
  senderId: 'u-customer',
  content: 'Bonjour',
  sentAt: '2026-11-27T14:41:00Z',
};

const agentMessage = {
  id: 'm2',
  conversationId: 'conv-1',
  senderId: 'u-agent',
  content: '<b>Je vous écoute</b>',
  sentAt: '2026-11-27T14:42:00Z',
};

describe('Chat', () => {
  let http: HttpTestingController;
  let socket: FakeChatSocketService;

  beforeEach(async () => {
    socket = new FakeChatSocketService();
    TestBed.configureTestingModule({
      imports: [Chat],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.overrideComponent(Chat, {
      set: { providers: [{ provide: ChatSocketService, useValue: socket }] },
    });
    await TestBed.compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Charge les utilisateurs de démo et affiche la vue demandée. */
  function createChat(mode: ChatMode = 'customer') {
    TestBed.inject(UserService).load();
    http.expectOne('/api/users').flush(demoUsers);
    const fixture = TestBed.createComponent(Chat);
    fixture.componentRef.setInput('id', 'conv-1');
    fixture.componentRef.setInput('mode', mode);
    fixture.detectChanges();
    return fixture;
  }

  /** Déclenche la connexion WebSocket et résout l'historique HTTP de la conversation. */
  async function initConversation(
    fixture: ReturnType<typeof createChat>,
    messages: unknown[] = [],
  ) {
    socket.events.next({ type: 'connected' });
    http.expectOne('/api/conversations/conv-1/messages').flush(messages);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  /** Saisit un texte dans le textarea et soumet le formulaire. */
  function submitMessage(fixture: ReturnType<typeof createChat>, content: string) {
    const field: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    field.value = content;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
    return field;
  }

  it("fusionne l'historique et les messages en direct sans doublon", async () => {
    const fixture = createChat();
    await initConversation(fixture, [customerMessage]);
    expect(fixture.nativeElement.querySelectorAll('.message').length).toBe(1);

    socket.events.next({ type: 'message', message: agentMessage });
    socket.events.next({ type: 'message', message: customerMessage });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.message').length).toBe(2);
  });

  it("affiche une erreur si la conversation n'existe pas", async () => {
    const fixture = createChat();
    socket.events.next({ type: 'connected' });
    http
      .expectOne('/api/conversations/conv-1/messages')
      .flush(null, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Conversation introuvable.',
    );
  });

  describe('vue client', () => {
    it('affiche les libellés côté client et échappe le HTML des messages', async () => {
      const fixture = createChat('customer');
      await initConversation(fixture, [customerMessage, agentMessage]);

      const text: string = fixture.nativeElement.textContent;
      expect(text).toContain('Vous (client), à');
      expect(text).toContain('Conseiller, à');
      expect(text).toContain('<b>Je vous écoute</b>');
    });

    it('envoie le message en tant que client puis vide le champ', async () => {
      const fixture = createChat('customer');
      await initConversation(fixture, []);

      const field = submitMessage(fixture, 'Bonjour');

      expect(socket.send).toHaveBeenCalledWith('conv-1', 'u-customer', 'Bonjour');
      expect(field.value).toBe('');
    });

    it('affiche le lien agent et le copie dans le presse-papiers', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      const fixture = createChat('customer');

      const link: HTMLAnchorElement = fixture.nativeElement.querySelector('.agent-link a');
      expect(link.getAttribute('href')).toMatch(/\/agent\/conv-1$/);

      fixture.nativeElement.querySelector('.agent-link button').click();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(writeText).toHaveBeenCalledWith(link.href);
      expect(fixture.nativeElement.querySelector('.agent-link output').textContent).toContain(
        'copié',
      );
    });
  });

  describe('vue agent', () => {
    it('affiche les libellés côté conseiller et masque le lien agent', async () => {
      const fixture = createChat('agent');
      await initConversation(fixture, [customerMessage, agentMessage]);

      const text: string = fixture.nativeElement.textContent;
      expect(text).toContain('Tchat conseiller');
      expect(text).toContain('Client, à');
      expect(text).toContain('Vous (conseiller), à');
      expect(fixture.nativeElement.querySelector('.agent-link')).toBeNull();
    });

    it('envoie le message en tant que conseiller puis vide le champ', async () => {
      const fixture = createChat('agent');
      await initConversation(fixture, []);

      const field = submitMessage(fixture, 'Bien reçu');

      expect(socket.send).toHaveBeenCalledWith('conv-1', 'u-agent', 'Bien reçu');
      expect(field.value).toBe('');
    });
  });
});
