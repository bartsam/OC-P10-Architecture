import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { UserService } from '../../core/services/user.service';
import { Chat } from './chat';

describe('Chat', () => {
  let http: HttpTestingController;

  /** Crée la vue, charge les utilisateurs de démo, puis attend la requête d'historique. */
  function createChat(mode: 'customer' | 'agent') {
    TestBed.inject(UserService).load();
    http.expectOne('/api/users').flush([
      { id: 'u-customer', firstName: 'John', lastName: 'Doe', role: 'CUSTOMER' },
      { id: 'u-agent', firstName: 'Jean', lastName: 'Biche', role: 'AGENT' },
    ]);
    const fixture = TestBed.createComponent(Chat);
    fixture.componentRef.setInput('id', 'conv-1');
    fixture.componentRef.setInput('mode', mode);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Chat],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("affiche l'historique avec l'auteur et l'heure", async () => {
    const fixture = createChat('customer');
    http.expectOne('/api/conversations/conv-1/messages').flush([
      {
        id: 'm1',
        conversationId: 'conv-1',
        senderId: 'u-customer',
        content: 'Bonjour',
        sentAt: '2026-11-27T14:41:00Z',
      },
      {
        id: 'm2',
        conversationId: 'conv-1',
        senderId: 'u-agent',
        content: '<b>Je vous écoute</b>',
        sentAt: '2026-11-27T14:42:00Z',
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Vous (client), à');
    expect(text).toContain('Conseiller, à');
    expect(text).toContain('<b>Je vous écoute</b>');
    expect(fixture.nativeElement.querySelector('.content b')).toBeNull();
  });

  it("affiche une erreur si la conversation n'existe pas", async () => {
    const fixture = createChat('customer');
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
