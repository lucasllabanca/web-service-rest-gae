package br.com.messenger.gae_service.controller;

import br.com.messenger.gae_service.exception.ProductOfInterestNotFoundException;
import br.com.messenger.gae_service.exception.UserNotFoundException;
import br.com.messenger.gae_service.model.ProductOfInterest;
import br.com.messenger.gae_service.model.User;
import br.com.messenger.gae_service.repository.ProductOfInterestRepository;
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

@RestController
@RequestMapping("/api/products-of-interest")
public class ProductOfInterestController {

    private static final Logger log = Logger.getLogger(UserController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductOfInterestRepository productOfInterestRepository;

    @Autowired
    CheckRole checkRole;

    @GetMapping(path = "/{cpf}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> getProductsOfInterest(@PathVariable String cpf, Authentication authentication) {

        String validateMsg = validateModel(Operation.GETBY, null, cpf, "cpf", null);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        if (canRunThisOperation(authentication, cpf)) {
            try {
                return new ResponseEntity<List<ProductOfInterest>>(productOfInterestRepository.getProductsOfInterestByCpf(cpf), HttpStatus.OK);
            } catch (UserNotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> saveProductOfInterest(@RequestBody ProductOfInterest productOfInterest, Authentication authentication) {

        String validateMsg = validateModel(Operation.SAVE, productOfInterest, null, null, null);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        if (canRunThisOperation(authentication, productOfInterest.getCpf())) {
            try {
                return new ResponseEntity<ProductOfInterest>(productOfInterestRepository.saveProductOfInterest(productOfInterest), HttpStatus.OK);
            } catch (UserNotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping(path = "/{cpf}/{salesProviderProductId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> deleteProductOfInterest(@PathVariable("cpf") String cpf, @PathVariable("salesProviderProductId") String salesProviderProductId, Authentication authentication) {

        String validateMsg = validateModel(Operation.DELETE, null, cpf, "cpf", false);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        validateMsg = validateModel(Operation.DELETE, null, salesProviderProductId, "salesProviderProductId", true);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        if (canRunThisOperation(authentication, cpf)) {
            try {
                return new ResponseEntity<ProductOfInterest>(productOfInterestRepository.deleteProductOfInterest(cpf, Long.parseLong(salesProviderProductId)), HttpStatus.OK);
            } catch (ProductOfInterestNotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
        }
    }

    private boolean canRunThisOperation(Authentication authentication, String cpf) {

        if (checkRole.hasRoleAdmin(authentication))
            return true;

        UserDetails authenticatedUser = (UserDetails) authentication.getPrincipal();
        Optional<User> optUser = userRepository.getByEmail(authenticatedUser.getUsername());

        if (optUser.isPresent())
            return (optUser.get().getCpf().equals(cpf));
        else
            return false;
    }

    private String validateModel(Operation operation, @Nullable ProductOfInterest productOfInterest, @Nullable String requestParam, @Nullable String paramName, @Nullable Boolean validateAsLong) {
        switch (operation) {
            case SAVE:
                return validateProductOfInterest(productOfInterest);

            case DELETE:
                if (paramName != null) {
                    if (requestParam == null || requestParam.trim().isEmpty())
                        return "Variável de endereço " + paramName + " deve ser informada.";

                    if (validateAsLong) {
                        try {

                            Long paramLong = Long.parseLong(requestParam);
                            if (paramLong == null || paramLong <= 0)
                                return "Variável de endereço " + paramName + " deve ser informada e não pode ser menor ou igual a 0.";

                        } catch (NumberFormatException e) {
                            return "Variável de endereço " + paramName + " deve ser um número inteiro.";
                        }
                    }
                }

            case GETBY:
                if (paramName != null && (requestParam == null || requestParam.trim().isEmpty()))
                    return "Variável de endereço " + paramName + " deve ser informada.";
        }

        return "";
    }

    private String validateProductOfInterest(ProductOfInterest productOfInterest) {

        if (productOfInterest.getCpf() == null || productOfInterest.getCpf().trim().isEmpty())
            return "Está faltando a propriedade 'cpf' no modelo.";

        if (productOfInterest.getSalesProviderUserId() == null)
            return "Está faltando a propriedade 'salesProviderUserId' no modelo.";

        if (productOfInterest.getSalesProviderUserId() <= 0)
            return "A propriedade 'salesProviderUserId' do modelo não pode ser menor ou igual a 0.";

        if (productOfInterest.getSalesProviderProductId() == null)
            return "Está faltando a propriedade 'ssalesProviderProductId' no modelo.";

        if (productOfInterest.getSalesProviderProductId() <= 0)
            return "A propriedade 'salesProviderProductId' do modelo não pode ser menor ou igual a 0.";

        if (productOfInterest.getMinPriceAlert() <= 0)
            return "A propriedade 'minPriceAlert' do modelo não pode ser 'null' e nem menor ou igual a 0.";

        return "";
    }
}
