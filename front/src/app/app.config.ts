import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { UserService } from './core/services/user.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding : paramètres et données de route alimentent les input() des composants
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(),
    // provideAppInitializer : Charge les utilisateurs de démo sans bloquer l'affichage
    provideAppInitializer(() => inject(UserService).load()),
  ],
};
