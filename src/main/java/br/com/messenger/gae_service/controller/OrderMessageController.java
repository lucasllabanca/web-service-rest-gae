package br.com.messenger.gae_service.controller;

import br.com.messenger.gae_service.model.Order;
import br.com.messenger.gae_service.model.User;
import br.com.messenger.gae_service.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Http;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/orders")
public class OrderMessageController {
    private static final Logger log = Logger.getLogger(OrderMessageController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://web-service-rest-gae.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp inicializado com sucesso!");
        } catch (IOException e) {
            log.info("Falha ao configurar FirebaseApp");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/message")
    public ResponseEntity<String> sendOrderMessage(@RequestBody Order order, @RequestParam String cpf) {

        Optional<User> optUser = userRepository.getByCpf(cpf);

        if (optUser.isPresent()) {

            User user = optUser.get();

            if (user.getFcmRegId() != null) {
                try {
                    Message message = Message.builder()
                            .putData("salesMessage", objectMapper.writeValueAsString(order))
                            .setToken(user.getFcmRegId())
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);

                    log.info("Notificação enviada para o usuário com cpf: " + user.getCpf() + " - com a messagem: " +order.getNotification());
                    log.info("Resposta do Firebase Cloud Messaging: " + response);

                    return new ResponseEntity<>("Notificação enviada com sucesso para o usuário com cpf: " + user.getCpf(), HttpStatus.OK);
                } catch (FirebaseMessagingException | JsonProcessingException e) {
                    return new ResponseEntity<>("Falha ao enviar notificação: " + e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>("Usuário com cpf: " + user.getCpf() + " - não registrado no FCM", HttpStatus.PRECONDITION_FAILED);
            }
        } else {
            return new ResponseEntity<>("Usuário com cpf: " + cpf + " - não encontrado", HttpStatus.NOT_FOUND);
        }
    }
}
