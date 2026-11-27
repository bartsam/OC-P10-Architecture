import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { Home } from './home';

describe('Home', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('affiche les conversations existantes avec un lien vers leur tchat', async () => {
    const fixture = TestBed.createComponent(Home);
    http
      .expectOne('/api/conversations')
      .flush([{ id: 'abc-000', userId: 'u1', createdAt: '2026-11-27T10:00:00Z' }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('li').length).toBe(1);
    expect(fixture.nativeElement.querySelector('li a').getAttribute('href')).toBe('/chat/abc-000');
  });

  it('crée une conversation puis ouvre la vue tchat', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Home);
    http.expectOne('/api/conversations').flush([]);
    await fixture.whenStable();

    fixture.nativeElement.querySelector('button').click();
    http
      .expectOne({ method: 'POST', url: '/api/conversations' })
      .flush({ id: 'abc-001', userId: 'u1', createdAt: '2026-11-27T11:00:00Z' });

    expect(navigate).toHaveBeenCalledWith(['/chat', 'abc-001']);
  });
});
