# Your Car Your Way : PoC du tchat

Preuve de concept (OpenClassrooms P10) du tchat en temps réel entre un client
et un agent du service client.

**Stack** : Spring Boot, Angular, PostgreSQL, WebSocket STOMP.

> Cette PoC démontre uniquement la viabilité des choix architecturaux et se focalise sur la fonctionnalité de tchat.

## Sommaire

1. Périmètre
2. Prérequis
3. Installation
4. Lancement
5. Démo
6. Fonctionnement
7. Arborescence
8. Structure des données
9. Dépannage

## 1. Périmètre

Cette PoC (preuve de concept) valide la viabilité des choix architecturaux de la future application Your Car Your Way à travers une seule fonctionnalité : le **tchat en temps réel entre un client et un agent du service client**. Elle ne construit pas l'application complète : tout ce qui n'est pas nécessaire au tchat est exclu, et l'interface est volontairement minimale.

### Inclus

- un client démarre une conversation ;
- un agent l'ouvre depuis un lien propre à la conversation ;
- le client et l'agent échangent en temps réel (WebSocket, protocole STOMP) ;
- les messages sont enregistrés en base et les utilisateurs retrouvent l'historique de leurs échanges.

### Exclus

| Fonctionnalité                  | PoC                                                                                                                                                                     | Cible                                                                                                                               |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Authentification et rôles       | Deux utilisateurs fictifs (`CUSTOMER` et `AGENT`) déterminés par la vue ouverte, sans mot de passe ni contrôle d'accès (quiconque dispose du lien agent peut répondre). | JWT à courte durée de vie, refresh token révocable dans Redis et vérification des rôles et droits côté serveur via Spring Security. |
| Plusieurs instances et Redis    | Une seule instance du back-end tourne avec le broker STOMP en mémoire de Spring.                                                                                        | Plusieurs instances derrière un load balancer, avec diffusion des messages entre elles via Redis pub/sub.                           |
| Conteneurs et déploiement cloud | Seule la base PostgreSQL tourne dans Docker, tandis que le back et le front restent en local.                                                                           | Conteneurs déployés par la CI/CD sur deux zones de disponibilité.                                                                   |
| Modèle de données complet       | Limité aux tables `user_account`, `conversation` et `message`, réduites aux colonnes nécessaires au tchat.                                                              | Schéma PostgreSQL unique couvrant utilisateurs, agences, véhicules, réservations, paiements et conversations.                       |
| Tests automatisés               | La vérification .                                                                                                                                                       | Schéma PostgreSQL unique couvrant utilisateurs, agences, véhicules, réservations, paiements et conversations.                       |

## 7. Arborescence

```text
.
├── back/    # API Spring Boot + WebSocket STOMP
├── front/   # Application Angular
└── README.md
```
