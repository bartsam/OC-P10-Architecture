package com.yourcaryourway.poc.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.jayway.jsonpath.JsonPath;
import com.yourcaryourway.poc.TestcontainersConfiguration;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.service.ChatService;

/**
 * Teste l'envoi d'un message de bout en bout : client STOMP → contrôleur →
 * service → base PostgreSQL → diffusion aux abonnés.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MessageControllerIntegrationTest {

  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final long TIMEOUT_SECONDS = 5;

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private ChatService chatService;

  // Broker STOMP en mémoire de Spring
  @Autowired
  private SimpleBrokerMessageHandler brokerHandler;

  // File d'attente où un thread WebSocket met chaque message diffusé sur le topic
  private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

  private StompSession session;
  private UUID conversationId;

  // Prépare une nouvelle conversation et un client abonné à son topic.
  @BeforeEach
  void connectAndSubscribe() throws Exception {
    // 1. Crée la conversation dans laquelle le test va écrire.
    conversationId = chatService.createConversation().id();

    // 2. Crée un client STOMP qui lit les messages JSON comme du texte brut
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new JsonStringMessageConverter());

    // 3. Ouvre la connexion WebSocket sur le vrai point d'entrée du serveur.
    session = client
        .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        })
        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // 4. S'abonne au topic : chaque message diffusé est rangé dans "received".
    session.subscribe(topic(), new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return String.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        received.add((String) payload);
      }
    });

    // 5. L'abonnement est asynchrone : on attend que le broker l'ait enregistré,
    // sinon un message envoyé trop tôt serait diffusé à personne.
    awaitSubscriptionRegistered();
  }

  // Ferme la session STOMP ouverte pour le test.
  @AfterEach
  void disconnect() {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  /** Un message valide est enregistré, puis diffusé avec les champs attendus. */
  @Test
  @DisplayName("Un message valide est enregistré puis diffusé aux abonnés")
  void validMessage_isSavedThenBroadcast() throws Exception {
    // GIVEN : un message d'un client connu.
    String message = json(CUSTOMER_ID, "Bonjour");

    // WHEN : il est envoyé, puis on attend sa diffusion (max TIMEOUT_SECONDS).
    send(message);
    String frame = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // THEN : le client reçoit le message complété par le serveur...
    assertThat(frame).as("aucun message diffusé").isNotNull();
    assertThat(JsonPath.<String>read(frame, "$.content")).isEqualTo("Bonjour");
    assertThat(JsonPath.<String>read(frame, "$.senderId")).isEqualTo(CUSTOMER_ID.toString());
    assertThat(JsonPath.<String>read(frame, "$.conversationId")).isEqualTo(conversationId.toString());
    assertThatCode(() -> Instant.parse(JsonPath.<String>read(frame, "$.sentAt")))
        .as("sentAt doit être une date ISO-8601")
        .doesNotThrowAnyException();

    // THEN : le message est bien enregistré en base.
    assertThat(chatService.findMessages(conversationId))
        .extracting(MessageResponse::content)
        .containsExactly("Bonjour");
  }

  /**
   * Un message invalide (vide, trop long, expéditeur inconnu) n'est ni diffusé
   * ni enregistré : seul le message valide envoyé ensuite est reçu.
   * 
   * @param invalidMessage corps JSON refusé par le serveur
   */
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidMessages")
  @DisplayName("Un message invalide n'est ni enregistré ni diffusé")
  void invalidMessage_isNeitherSavedNorBroadcast(String invalidMessage) throws Exception {
    // GIVEN : un message invalide (fourni par invalidMessages) et un témoin valide.
    String witnessMessage = json(CUSTOMER_ID, "ok");

    // WHEN : les deux sont envoyés à la suite.
    send(invalidMessage);
    send(witnessMessage);
    String frame = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // THEN : le 1er message reçu est le témoin, donc l'invalide n'a pas été diffusé
    assertThat(frame).as("le témoin n'est pas arrivé").isNotNull();
    assertThat(JsonPath.<String>read(frame, "$.content")).isEqualTo("ok");

    // THEN : on vérifie que rien n'arrive après le témoin (en cas de délais)
    assertThat(received.poll(500, TimeUnit.MILLISECONDS)).isNull();

    // THEN : seul le témoin est enregistré en base.
    assertThat(chatService.findMessages(conversationId))
        .extracting(MessageResponse::content)
        .containsExactly("ok");
  }

  /**
   * Destination sur laquelle le serveur diffuse les messages de la conversation
   * 
   * @return la destination de diffusion de la conversation du test
   */
  private String topic() {
    return "/topic/conversations/" + conversationId;
  }

  /**
   * Cas de message refusés par le serveur.
   * 
   * {@link Named} associe un libellé lisible à la place de {@code {0}} dans le
   * nom du test, tout en passant le JSON en paramètre
   * 
   * @return des corps de message que le serveur doit refuser
   */
  static Stream<Named<String>> invalidMessages() {
    return Stream.of(
        Named.of("contenu vide", json(CUSTOMER_ID, "   ")),
        Named.of("contenu trop long", json(CUSTOMER_ID, "a".repeat(2001))),
        Named.of("expéditeur inconnu", json(UUID.randomUUID(), "expéditeur inconnu")));
  }

  /**
   * Attend que le broker ait enregistré l'abonnement au topic avant d'envoyer un
   * message, en vérifiant toutes les 25 ms qu'au moins un abonné est présent.
   */
  private void awaitSubscriptionRegistered() {
    // 1. Prépare les en-têtes d'un message ciblant le topic de la conversation.
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setDestination(topic());

    // 2. Construit une "sonde" vide portant ces en-têtes (jamais envoyée)
    // pour interroger le registre d'abonnements du broker.
    Message<byte[]> probe = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    // 3. Répète la vérification toutes les 25 ms jusqu'à ce que le broker
    // trouve un abonnement correspondant à la destination de la sonde
    await()
        .atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
        .pollInterval(Duration.ofMillis(25))
        .until(() -> !brokerHandler.getSubscriptionRegistry().findSubscriptions(probe).isEmpty());
  }

  /**
   * Envoie un message JSON vers {@code /app/conversations/{id}/messages}.
   *
   * @param json corps du message
   */
  private void send(String json) {
    StompHeaders headers = new StompHeaders();
    headers.setDestination("/app/conversations/" + conversationId + "/messages");
    headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
    session.send(headers, json);
  }

  /**
   * Construit le corps JSON d'un message.
   *
   * @param senderId identifiant de l'expéditeur
   * @param content  texte du message
   * @return le JSON {@code {senderId, content}}
   */
  private static String json(UUID senderId, String content) {
    return "{\"senderId\":\"" + senderId + "\",\"content\":\"" + content + "\"}";
  }

  /**
   * Convertisseur du client de test : lit le corps des messages STOMP comme
   * une chaîne de caractères, sans le transformer en objet.
   *
   * {@link StringMessageConverter} n'accepte que {@code text/plain} par défaut ;
   * on lui ajoute {@code application/json} pour recevoir les messages du serveur.
   */
  private static final class JsonStringMessageConverter extends StringMessageConverter {

    JsonStringMessageConverter() {
      addSupportedMimeTypes(MimeTypeUtils.APPLICATION_JSON);
    }
  }
}