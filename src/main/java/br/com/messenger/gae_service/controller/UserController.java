package br.com.messenger.gae_service.controller;

import br.com.messenger.gae_service.exception.UserAlreadyExistsException;
import br.com.messenger.gae_service.exception.UserNotFoundException;
import br.com.messenger.gae_service.model.User;
import br.com.messenger.gae_service.repository.UserRepository;
import br.com.messenger.gae_service.util.CheckRole;
import br.com.messenger.gae_service.util.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = Logger.getLogger(UserController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    CheckRole checkRole;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        return new ResponseEntity<List<User>>(userRepository.getUsers(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody User user) {

        String validateMsg = validateModel(Operation.SAVE, user, null, null);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        try {
            return new ResponseEntity<User>(userRepository.saveUser(user), HttpStatus.OK);
        } catch (UserAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PutMapping(path = "/byemail")
    public ResponseEntity<?> updateUser(@RequestBody User user, @RequestParam String email, Authentication authentication) {

        String validateMsg = validateModel(Operation.UPDATE, user, email, "email");

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        if (canRunThisOperation(authentication, email)) {

            if (!checkRole.hasRoleAdmin(authentication))
                user.setRole("ROLE_USER");

            try {
                return new ResponseEntity<User>(userRepository.updateUser(user, email, true, true, false), HttpStatus.OK);
            } catch (UserAlreadyExistsException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
            } catch (UserNotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @GetMapping("/byemail")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email, Authentication authentication) {

        String validateMsg = validateModel(Operation.GETBY, null, email, "email");

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        if (canRunThisOperation(authentication, email)) {
            Optional<User> optUser = userRepository.getByEmail(email);

            if (optUser.isPresent()){
                return new ResponseEntity<User>(optUser.get(), HttpStatus.OK);
            }else {
                return new ResponseEntity<>("Usuário com email: " + email + " - não encontrado", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @GetMapping("/bycpf")
    public ResponseEntity<?> getUserByCpf(@RequestParam String cpf, Authentication authentication) {

        String validateMsg = validateModel(Operation.GETBY, null, cpf, "cpf");

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        Optional<User> optUser = userRepository.getByCpf(cpf);

        if (optUser.isPresent()){
            User user = optUser.get();

            if (canRunThisOperation(authentication, user.getEmail()))
                return new ResponseEntity<User>(user, HttpStatus.OK);
            else
                return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }else {
            return new ResponseEntity<>("Usuário com cpf: " + cpf + " - não encontrado", HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @DeleteMapping(path = "/bycpf")
    public ResponseEntity<?> deleteUser(@RequestParam String cpf, Authentication authentication) {

        String validateMsg = validateModel(Operation.DELETE, null, cpf, "cpf");

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        Optional<User> optUser = userRepository.getByCpf(cpf);

        if (optUser.isPresent()){

            User user = optUser.get();

            if (canRunThisOperation(authentication, user.getEmail())) {
                try {
                    return new ResponseEntity<User>(userRepository.deleteUser(cpf), HttpStatus.OK);
                } catch (UserNotFoundException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
            }

        } else {
            return new ResponseEntity<>("Usuário com cpf: " + cpf + " - não encontrado", HttpStatus.NOT_FOUND);
        }
    }

    private boolean canRunThisOperation(Authentication authentication, String email) {
        return (checkRole.hasRoleAdmin(authentication) || getAuthenticationEmail(authentication).equals(email));
    }

    private String getAuthenticationEmail(Authentication authentication) {
        UserDetails user = (UserDetails) authentication.getPrincipal();
        return user.getUsername();
    }

    private String validateModel(Operation operation, @Nullable User user, @Nullable String requestParam, @Nullable String paramName) {
        switch (operation) {
            case SAVE:
                if (user.getPassword() == null || user.getPassword().trim().isEmpty())
                    return "Está faltando a propriedade 'password' no modelo.";
                else
                    return validateUser(user);

            case UPDATE:
                if (user.getId() == null || user.getId() == 0)
                    return "Está faltando a propriedade 'id' no modelo.";
                else if (paramName != null && (requestParam == null || requestParam.trim().isEmpty()))
                    return "Parâmetro de requisição " + paramName + " deve ser informado.";
                else
                    return validateUser(user);

            default:
                if (paramName != null && (requestParam == null || requestParam.trim().isEmpty()))
                    return "Parâmetro de requisição " + paramName + " deve ser informado.";
        }

        return "";
    }

    private String validateUser(User user) {

        if (user.getEmail() == null || user.getEmail().trim().isEmpty())
            return "Está faltando a propriedade 'email' no modelo.";

        if (!validateEmail(user.getEmail()))
            return "A propriedade 'email' do modelo não é um endereço de e-mail válido.";

        if (user.getRole() == null || user.getRole().trim().isEmpty())
            return "Está faltando a propriedade 'role' no modelo.";

        if (!user.getRole().equals("ROLE_ADMIN") && !user.getRole().equals("ROLE_USER"))
        return "Propriedade 'role' inválida. Role deve ser 'ROLE_ADMIN' ou 'ROLE_USER'.";

        if (user.getCpf() == null || user.getCpf().trim().isEmpty())
            return "Está faltando a propriedade 'cpf' no modelo.";

        if (user.getLastFcmRegister() != null)
            return "A propriedade 'lastFcmRegister' do modelo não deve ser passada na requisição pois é registrada sempre que a propriedade 'fcmRegId' é alterada.";

        if (user.getLastLogin() != null)
            return "A propriedade 'lastLogin' do modelo não precisa ser passada na requisição pois é registrada sempre que o usuário loga na aplicação e o cache não é usado.";

        if (user.getLastUpdate() != null)
            return "A propriedade 'lastUpdate' do modelo não precisa ser passada na requisição pois é registrada sempre que o usuário é criado ou alterado.";

        return "";
    }

    private boolean validateEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
