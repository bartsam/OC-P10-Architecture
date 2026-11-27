import { Routes } from '@angular/router';

import { Chat } from './features/chat/chat';
import { Home } from './features/home/home';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'chat/:id', component: Chat, data: { mode: 'customer' } },
  { path: 'agent/:id', component: Chat, data: { mode: 'agent' } },
  { path: '**', redirectTo: '' },
];
