import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { vi } from 'vitest';

import { ChatEvent } from '../../core/models/chat.model';
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

  /** Charge les utilisateurs de démonstration et affiche la vue client de `conv-1`. */
  function createChat() {
    TestBed.inject(UserService).load();
    http.expectOne('/api/users').flush([
      { id: 'u-customer', firstName: 'John', lastName: 'Doe', role: 'CUSTOMER' },
      { id: 'u-agent', firstName: 'Jean', lastName: 'Biche', role: 'AGENT' },
    ]);
    const fixture = TestBed.createComponent(Chat);
    fixture.componentRef.setInput('id', 'conv-1');
    fixture.componentRef.setInput('mode', 'customer');
    fixture.detectChanges();
    return fixture;
  }

  it("fusionne l'historique et les messages en direct sans doublon", async () => {
    const fixture = createChat();
    socket.events.next({ type: 'connected' });
    http.expectOne('/api/conversations/conv-1/messages').flush([customerMessage]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.message').length).toBe(1);

    socket.events.next({ type: 'message', message: agentMessage });
    socket.events.next({ type: 'message', message: customerMessage });
    fixture.detectChanges();

    const text: string = fixture.nativeElement.textContent;
    expect(fixture.nativeElement.querySelectorAll('.message').length).toBe(2);
    expect(text).toContain('Vous (client), à');
    expect(text).toContain('Conseiller, à');
    expect(text).toContain('<b>Je vous écoute</b>');
  });

  it('envoie le message saisi puis vide le champ', async () => {
    const fixture = createChat();
    socket.events.next({ type: 'connected' });
    http.expectOne('/api/conversations/conv-1/messages').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const field: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    field.value = 'Bonjour';
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(socket.send).toHaveBeenCalledWith('conv-1', 'u-customer', 'Bonjour');
    expect(field.value).toBe('');
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
});
