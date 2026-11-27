package com.yourcaryourway.poc.chat.controller;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.yourcaryourway.poc.TestcontainersConfiguration;
import com.yourcaryourway.poc.chat.dto.MessageRequest;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.service.ChatService;

/**
 * Test d'intégration du tchat :
 * client STOMP → contrôleur → service → base PostgreSQL → diffusion abonnés
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MessageControllerIntegrationTest {

  /** Client de démonstration inséré par la migration Flyway V2. */
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final long TIMEOUT_SECONDS = 5;

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private ChatService chatService;

  @Autowired
  private SimpleBrokerMessageHandler broker;

  /** File d'attente des messages reçus par le client abonné. */
  private final BlockingQueue<MessageResponse> received = new LinkedBlockingQueue<>();

  private StompSession session;
  private UUID conversationId;

  @BeforeEach
  void connectAndSubscribe() throws Exception {
    // 1. Crée la conversation du test.
    conversationId = chatService.createConversation().id();

    // 2. Crée un client STOMP qui convertit les messages depuis et vers JSON.
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new JacksonJsonMessageConverter());

    // 3. Se connecte au point d'entrée WebSocket /ws.
    session = client
        .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        })
        .get(TIMEOUT_SECONDS, SECONDS);

    // 4. S'abonne au topic : chaque message diffusé est rangé dans "received".
    session.subscribe(topic(), new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return MessageResponse.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        received.add((MessageResponse) payload);
      }
    });

    // 5. Attend l'enregistrement de l'abonnement par le broker avant tout envoi.
    await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS)).until(this::isSubscribed);
  }

  /** Ferme la session STOMP ouverte pour le test. */
  @AfterEach
  void disconnect() {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  @Test
  @DisplayName("Un message valide est enregistré puis diffusé aux abonnés")
  void validMessage_isSavedThenBroadcast() throws Exception {
    // GIVEN : un message d'un client connu.
    MessageRequest request = new MessageRequest(CUSTOMER_ID, "Bonjour");

    // WHEN : il est envoyé à la conversation.
    session.send(destination(), request);
    MessageResponse response = received.poll(TIMEOUT_SECONDS, SECONDS);

    // THEN : il est diffusé, complété par le serveur
    assertThat(response).as("aucun message diffusé").isNotNull();
    assertThat(response.content()).as("contenu diffusé").isEqualTo("Bonjour");
    assertThat(response.senderId()).as("expéditeur diffusé").isEqualTo(CUSTOMER_ID);
    assertThat(response.conversationId()).as("conversation diffusée").isEqualTo(conversationId);
    assertThat(response.id()).as("id attribué par le serveur").isNotNull();
    assertThat(response.sentAt()).as("date d'envoi fixée par le serveur").isNotNull();

    // THEN : puis il est enregistré en base.
    assertThat(chatService.findMessages(conversationId))
        .as("le message diffusé doit être celui enregistré en base")
        .extracting(MessageResponse::id)
        .containsExactly(response.id());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  @DisplayName("Un message invalide n'est ni enregistré ni diffusé")
  void invalidMessage_isNeitherSavedNorBroadcast(MessageRequest invalidMessage) throws Exception {
    // GIVEN : un message invalide, puis un message valide "témoin".
    MessageRequest validMessage = new MessageRequest(CUSTOMER_ID, "ok");

    // WHEN : les deux sont envoyés à la suite.
    session.send(destination(), invalidMessage);
    session.send(destination(), validMessage);
    MessageResponse firstMessage = received.poll(TIMEOUT_SECONDS, SECONDS);

    // THEN : seul le "témoin" est diffusé (rien n'arrive avant ni après lui)...
    assertThat(firstMessage).as("le témoin n'est pas arrivé").isNotNull();
    assertThat(firstMessage.content()).as("le premier message reçu doit être le témoin").isEqualTo("ok");
    assertThat(received.poll(500, MILLISECONDS))
        .as("aucun message ne doit arriver après le témoin")
        .isNull();

    // THEN : puis seul le "témoin" est enregistré.
    assertThat(chatService.findMessages(conversationId))
        .as("seul le témoin doit être enregistré en base")
        .extracting(MessageResponse::content)
        .containsExactly("ok");
  }

  /**
   * Fournit les messages que le serveur doit refuser.
   * {@link Named} associe à chacun un label, affiché à la place de {@code {0}}
   *
   * @return les messages invalides : contenu vide, trop long, expéditeur inconnu
   */
  static Stream<Named<MessageRequest>> invalidRequests() {
    return Stream.of(
        Named.of("contenu vide", new MessageRequest(CUSTOMER_ID, "   ")),
        Named.of("contenu trop long", new MessageRequest(CUSTOMER_ID, "a".repeat(2001))),
        Named.of("expéditeur inconnu", new MessageRequest(UUID.randomUUID(), "inconnu")));
  }

  /**
   * Vérifie si le broker a enregistré au moins
   * un abonné au topic de la conversation.
   *
   * @return {@code true} si au moins une session est abonnée au topic
   */
  private boolean isSubscribed() {
    // Crée un accesseur d'en-têtes pour un message STOMP
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    // Définit la destination (topic) à tester
    accessor.setDestination(topic());
    // Construit un message "sonde" vide avec ces en-têtes
    Message<byte[]> probe = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    // Renvoie vrai si au moins un abonnement correspond à la destination
    return !broker.getSubscriptionRegistry().findSubscriptions(probe).isEmpty();
  }

  /**
   * Adresse d'envoi d'un message : le préfixe {@code /app} la route vers
   * {@code MessageController}.
   *
   * @return la destination d'envoi de la conversation du test
   */
  private String destination() {
    return "/app/conversations/" + conversationId + "/messages";
  }

  /**
   * Adresse de diffusion : le serveur y publie chaque message accepté,
   * et les clients s'y abonnent pour le recevoir.
   *
   * @return le topic de la conversation du test
   */
  private String topic() {
    return "/topic/conversations/" + conversationId;
  }
}